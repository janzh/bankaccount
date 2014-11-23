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
	// <id of transaction, value of proposal>
	private Map<Integer, Integer> proposals;
	// Phase1 list of received prepare responses
	private ArrayList<PrepareResponseMessage> prepResponseList;
	// Phase2 list of received accepted notifications
	private ArrayList<AcceptNotificationMessage> acceptNotificationList;
	
	private ArrayList<Replica> replicas;
	private NodeLocationData locationData;
	private Message message;
	private ServerListener serverListener;
	
	public Replica(String host, int port, int id){
		this.id = id;
		this.ballotNum = new Pair();
		this.acceptNum = new Pair();

		account = new Account();
		log = new Log();
		replicas = new ArrayList<>();
		isAlive = true;
		
		locationData = new NodeLocationData(host, port, id);
		serverListener = new ServerListener(this);
		serverListener.start();
		
		prepResponseList = new ArrayList<PrepareResponseMessage>();
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
		account.deposit(value);
		fireActionPerformed(Type.DEPOSIT, Status.SUCCESS);
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
		locationData.becomeLeader();
		for (Replica replica : replicas) {
			if(!replica.equals(this)) {
				replica.getLocationData().becomeNonLeader();
			}
			else {replica.getLocationData().becomeLeader();}
		}
		// Remove all responses, since we are starting a new phase1
		prepResponseList.clear();
		
		Pair newBallot = new Pair();
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
			// TODO: Should not run phase 1 when leader has not changes, so proposeToLeaderMessage should else go to Phase2
			
			ProposeToLeaderMessage proposeMessage = (ProposeToLeaderMessage)m;
			if(locationData.isLeader()) {
				// Store all received transaction-proposals in a Map
				proposals.put(proposeMessage.getId(), proposeMessage.getValue());
				AcceptRequestMessage acceptReqMessage = new AcceptRequestMessage(this.ballotNum, proposeMessage.getValue());
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
			// TODO: Needs to handle different messages for different Ballots
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
				if(highestVal == 0) {myVal = initVal;} // TODO: find a way to store init proposed value
				else {
					myVal = highestResponse.getAcceptVal();
				}
				AcceptRequestMessage acceptReqMessage = new AcceptRequestMessage(prepareResponse.getBallotNum(), myVal);
				acceptReqMessage.setSender(this.locationData);
				broadcast(acceptReqMessage);
				
			}
			else {return;}
		}

		else if(m instanceof AcceptRequestMessage) {
			AcceptRequestMessage acceptRequest = (AcceptRequestMessage)m;
			Pair bal = acceptRequest.getBallotNum();
			if(isBallotBigger(bal, this.ballotNum)) {
				this.acceptNum = bal;
				this.acceptVal = acceptRequest.getValue();
				AcceptNotificationMessage acceptNotification = new AcceptNotificationMessage(this.acceptNum, this.acceptVal);
				acceptNotification.setSender(this.locationData);
				unicast(acceptRequest.getSender() ,acceptNotification); 
				// TODO: Broadcast first time?, unicast the rest?
			}
			else {return;}
		}

		else if(m instanceof AcceptNotificationMessage) {
			// TODO: Needs to handle different messages for different Ballots
			AcceptNotificationMessage acceptNot = (AcceptNotificationMessage)m;
			acceptNotificationList.add(acceptNot);
			// Check if notification has come from a majority
			if(acceptNotificationList.size() > (replicas.size() / 2)){
				// Decide value
				decide(acceptNot.getValue());
				
				DecideMessage decideMessage = new DecideMessage(acceptNot.getValue());
				decideMessage.setSender(this.locationData);
				broadcast(decideMessage);
			}
			else{return;}
		}
		else if(m instanceof DecideMessage){
			DecideMessage decideMsg = (DecideMessage)m;
			decide(decideMsg.getValue());
		}
	}
	/*
	 * Decide should update local log, replica and status of transaction in CLI
	 * @Param value - value that Paxos Algorithm decided
	 */
	private void decide(double value) {
		// TODO Implement method	
	}
	/* 
	 * Send message to one receiver
	 * @Param m - Message to be sent
	 * @Param nodeLocationData - Data of location of the node to receive message 
	 */
	private void unicast(NodeLocationData nodeLocationData, Message m) {
		// TODO: Implement method
		if(!isAlive) {return;}
	}
	private void broadcast(Message m) {
		// TODO: Implement method
		if(!isAlive) {return;}
		
		m.setSender(locationData);
		
		for(Replica replica : replicas)
		{
			// immediately deliver to self
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
}
