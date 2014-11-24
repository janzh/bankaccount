package bankaccount;

import java.util.ArrayList;
import java.util.Map;

import bankaccount.ReplicaEvent.Status;
import bankaccount.ReplicaEvent.Type;

public class Replica {
	private Account account;
	private Log log;
	private ReplicaListener listener;
	private boolean isAlive;
	private int id;
	// Latest ballot pi took part in
	private Pair ballotNum;
	// Latest ballot pi accepted a value
	private Pair acceptNum;
	// Latest accepted value
	private double acceptVal;
	
	// Proposer Variables
	private ArrayList<Proposal> myProposals; // list of proposals made by this replica
	private ArrayList<Proposal> receivedProposals; // list of proposals received as leader
	
	// Phase1 list of received prepare responses
	private ArrayList<PrepareResponseMessage> prepResponseList;
	// Phase2 list of received accepted notifications
	// Acceptor Variables
	private ArrayList<AcceptNotificationMessage> acceptNotificationList;
	// Learner variables
	private ArrayList<Proposal> learnedProposals;
	
	private ArrayList<Replica> replicas;
	private NodeLocationData locationData;
	private Message message;
	private ServerListener serverListener;
	
	public Replica(String host, int port, int id){
		this.id = id;
		this.ballotNum = new Pair(id);
		this.acceptNum = new Pair(id);
		this.acceptVal = 0;

		account = new Account();
		log = new Log();
		replicas = new ArrayList<>();
		isAlive = true;
		
		locationData = new NodeLocationData(host, port, id);
		serverListener = new ServerListener(this);
		serverListener.start();
		
		myProposals = new ArrayList<Proposal>();
		receivedProposals = new ArrayList<Proposal>();
		prepResponseList = new ArrayList<PrepareResponseMessage>();
		acceptNotificationList = new ArrayList<AcceptNotificationMessage>();
		learnedProposals = new ArrayList<Proposal>();
	}

	public void setListener(ReplicaListener listener){
		this.listener = listener;	
	}
	public NodeLocationData getLocationData(){
		return locationData;
	}
	public void setReplicaList(ArrayList<Replica> replicas){
		this.replicas = replicas;
	}
	public ArrayList<Replica> getReplicaList(){
		return replicas;
	}
	public int getId(){
		return id;
	}
	public boolean isAlive(){
		return isAlive;
	}
	public Log getLog(){
		return log;
	}
	
	public void deposit(double value){
		Proposal proposal;
		proposal = new Proposal(this.id, myProposals.size(), value, "d");
		myProposals.add(proposal);
		ProposeToLeaderMessage proposeMsg = new ProposeToLeaderMessage(proposal);
		proposeMsg.setSender(this.locationData);
		unicast(getCurrentLeader().getLocationData(), proposeMsg);
		
	}
	public void withdraw(double value){
		account.withdraw(value);
		fireActionPerformed(Type.WITHDRAW, Status.FAIL);
	}
	public void balance(){
		fireActionPerformed(Type.BALANCE, Status.SUCCESS);
	}
	public void fail(){
		isAlive = false;
		fireActionPerformed(Type.FAIL, Status.SUCCESS);
	}
	public void unfail(){
		isAlive = true;
		fireActionPerformed(Type.UNFAIL, Status.SUCCESS);
	}
	
	private void fireActionPerformed(Type type, Status status){
		if (listener != null) listener.replicaActionPerformed(new ReplicaEvent(type, status, account.getBalance()));
	}
	private void fireActionPerformed(Type type, Message message){
		if (listener != null) listener.replicaActionPerformed(new ReplicaEvent(type, message));
		deliver(message);
	}

	public void setMessage(Message receivedMessage) {
		this.message = receivedMessage;
		fireActionPerformed(Type.RECEIVE, message);	
	}
	/*
	 * Set itself to leader, an updates so that list of other replicas are not leader
	 * Start phase1 of Paxos algorithm
	 */
	public void becomeLeader() {
		// Makes itself leader, and update list of replicas accordingly
		locationData.becomeLeader();
		for (Replica replica : replicas) {
			if(!replica.equals(this)) {
				replica.getLocationData().becomeNonLeader();
			}
			else {replica.getLocationData().becomeLeader();}
		}
		// Remove all responses, since we are starting a new phase1
		prepResponseList.clear();
		
		Pair newBallot = new Pair(this.id);
		newBallot.setBallotNum(this.ballotNum.getBallotNum() + 1);
		newBallot.setId(this.id);
		PrepareRequestMessage prepareRequest = new PrepareRequestMessage(newBallot);
		prepareRequest.setSender(this.locationData);
		broadcast(prepareRequest);
	}

	private synchronized void deliver(Message m) {
		if(!isAlive) {
			return;
		}
		else if(m instanceof ProposeToLeaderMessage) {
			// TODO: Should not run phase 1 when leader has not changed, so proposeToLeaderMessage should else go to Phase2
			System.out.println(this.id + ": ProposeToLeaderMessage received at replica");
			
			ProposeToLeaderMessage proposeMessage = (ProposeToLeaderMessage)m;
			if(locationData.isLeader()) {
				// Store all received transaction-proposals in a Map
				receivedProposals.add(proposeMessage.getProposal());
				AcceptNotificationMessage acceptReqMessage = new AcceptNotificationMessage(this.ballotNum, proposeMessage.getProposal());
				acceptReqMessage.setSender(this.locationData);
				broadcast(acceptReqMessage);

			}
			else {return;}
		}
		else if(m instanceof PrepareRequestMessage) {
			PrepareRequestMessage prepareRequest = (PrepareRequestMessage)m;
			Pair bal = prepareRequest.getBallotNum();
			if (isBallotBigger(bal, this.ballotNum)) {
				this.ballotNum = bal;
				PrepareResponseMessage prepareResponse = new PrepareResponseMessage(this.ballotNum, this.acceptNum, this.acceptVal);
				prepareResponse.setSender(this.locationData);
				unicast(prepareRequest.getSender(), prepareResponse);
			}
			else {return;}
		}

		else if(m instanceof PrepareResponseMessage) {
			PrepareResponseMessage prepareResponse = (PrepareResponseMessage)m;
			prepResponseList.add(prepareResponse);
			
			// Check if response has come from a majority
			if(prepResponseList.size() > (replicas.size() / 2)) {
				double highestVal = 0;
				double myVal;
				PrepareResponseMessage highestResponse = null;
				for (PrepareResponseMessage response : prepResponseList) {
					double tempVal = response.getAcceptVal();
					if(tempVal > highestVal) {
						highestVal = tempVal;
					}
					if(highestResponse.equals(null)){
						highestResponse = response;
					}
					else if(isBallotBigger(response.getAcceptNum(), highestResponse.getBallotNum())){
						highestResponse = response;
					}
				}
				if(highestVal == 0) {myVal = 0;} // TODO: find a way to store init proposed value
				else {
					myVal = highestResponse.getAcceptVal();
				}
				AcceptNotificationMessage acceptReqMessage = new AcceptNotificationMessage(prepareResponse.getBallotNum(), myVal);
				acceptReqMessage.setSender(this.locationData);
				broadcast(acceptReqMessage);
				
			}
			else {return;}
		}

		else if(m instanceof AcceptNotificationMessage) {
			
			AcceptNotificationMessage acceptNot = (AcceptNotificationMessage)m;
			Pair bal = acceptNot.getBallotNum();
			// If it is first time it receive the acceptMsg(it's sent from the leader)
			if(!receivedAcceptNot(acceptNot)) {
				System.out.println(this.id + ": 1.st acceptNotificationMessage received at replica");
				
				acceptNotificationList.add(acceptNot);
				if(isBallotBigger(bal, this.ballotNum)) {
					this.acceptNum = bal;
					this.acceptVal = acceptNot.getProposal().getValue();
					AcceptNotificationMessage acceptNotification = new AcceptNotificationMessage(acceptNot.getBallotNum(), acceptNot.getProposal());
					acceptNotification.setSender(this.locationData);
					broadcast(acceptNotification); 
					// TODO: Broadcast first time?, unicast the rest?
				}
				else {return;}
			}
			else if(receivedAcceptNot(acceptNot)){
				System.out.println(this.id + ": acceptNotificationMessage Received at replica");
				
				acceptNotificationList.add(acceptNot);
				// Check if notification has come from a majority
				if(receivedExtMajorityAcceptors(acceptNot) ) {
					// Decide value
					DecideMessage decideMessage = new DecideMessage(acceptNot.getProposal());
					decideMessage.setSender(this.locationData);
					broadcast(decideMessage);
				}
				else {return;}
			}
		}
		
		else if(m instanceof DecideMessage){
			System.out.println(this.id + ": DecideMessage received at replica");
			DecideMessage decideMsg = (DecideMessage)m;
			Proposal proposal = decideMsg.getProposal();
			// Only learn value if it has not already
			if(!receivedDecideMsg(decideMsg)) {
				learnedProposals.add(proposal);
				decide(proposal);
			}
		}
	}
	/*
	 * Decide should update local log, replica and status of transaction in CLI
	 * @Param value - value that Paxos Algorithm decided
	 */
	private void decide(Proposal proposal) {
		System.out.println(this.id + ": Value: " + proposal.getValue() + " has been learned");
		System.out.println("------------------------------------------------------");
		
		log.putElement(Double.toString(proposal.getValue()));
		// Perform transaction withdrawal
		if(proposal.getType().equals("w")) {
			account.withdraw(proposal.getValue());
		}
		// Perform transaction deposit
		else if(proposal.getType().equals("d")) {
			account.deposit(proposal.getValue());
		}
		if(proposal.getProposerId() == this.id) {
			fireActionPerformed(Type.DEPOSIT, Status.SUCCESS);
		}
	}
	/* 
	 * Send message to one receiver
	 * @Param m - Message to be sent
	 * @Param nodeLocationData - Data of location of the node to receive message 
	 */
	private void unicast(NodeLocationData nodeLocationData, Message m) {
		if(!isAlive) {return;}
		Communication.sendMessage(nodeLocationData, m);
	}
	private void broadcast(Message m) {
		if(!isAlive) {return;}
		
		for(Replica replica : replicas)
		{
			// immediately deliver to self, but not if DecideMessage, because value has already been decide locally
			if(this.locationData.equals(replica.getLocationData())) {
				deliver(m);
			}
			// send message
			else {
				unicast(replica.getLocationData(), m);
			}
		}
	}
	/*
	 * Return true if ballot 1 is higher than ballot 2
	 */
	private boolean isBallotBigger(Pair bal1, Pair bal2) {
		if(bal1.getBallotNum() > bal2.getBallotNum()) {
			return true;
		}
		else if(bal1.getBallotNum() == bal2.getBallotNum() && bal1.getId() > bal2.getId()){
			return true;
		}
		return false;
	}
	
	private boolean receivedExtMajorityAcceptors(AcceptNotificationMessage acceptNot){
		int counter = 0;
		for(int i = 0; i < acceptNotificationList.size(); i++){
			if((acceptMsgsAreEqual(acceptNotificationList.get(i), acceptNot))) {
				counter++;
			}
		}
		if (counter == ((replicas.size() / 2)) + 1) {
			return true;
		}
		else {return false;}
	}
	
	private Replica getCurrentLeader(){
		for (int i = 0; i < replicas.size(); i++) {
			Replica tempReplica = replicas.get(i);
			if(tempReplica.locationData.isLeader()) {
				return tempReplica;
			}
		}
		return null;
	}
	
	private boolean receivedAcceptNot(AcceptNotificationMessage acceptNot) {
		for(int i = 0; i < acceptNotificationList.size(); i++){
			if(acceptMsgsAreEqual(acceptNotificationList.get(i), acceptNot)) {
				return true;
			}
		}
		return false;
	}
	private boolean acceptMsgsAreEqual(AcceptNotificationMessage msg1, AcceptNotificationMessage msg2) {
		if (msg1.getBallotNum().getBallotNum() == msg2.getBallotNum().getBallotNum()) {
			if(msg1.getBallotNum().getId() == msg2.getBallotNum().getId()) {
				if(msg1.getProposal().isEqual(msg2.getProposal())) {
					return true;
				}
			}
		}
		return false;
	}
	private boolean receivedDecideMsg(DecideMessage msg){
		for(int i = 0; i < learnedProposals.size(); i++){
			if(learnedProposals.get(i).isEqual(msg.getProposal())) {
				return true;
			}
		}
		return false;
	}
}
