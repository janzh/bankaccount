package bankaccount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import bankaccount.ReplicaEvent.Status;
import bankaccount.ReplicaEvent.Type;
import bankaccount.log.Log;
import bankaccount.log.LogEntry;
import bankaccount.messages.*;

public class Replica {
	private static final int nrOfReplicas = 5;
	
	private Account account;
	private Log log;
	private ReplicaListener listener;
	private boolean isAlive;
	// True is it has received a majority of ACK's for prepare requests
	private boolean hasMajorityBallot;
	private int id;
	// Latest ballot pi took part in
	private Pair ballotNum;
	// Latest ballot pi accepted a value
	private Pair acceptNum;
	// Latest accepted value
	private double acceptVal;
	
	// Proposer Variables
	private ArrayList<Proposal> notAcceptedProposals;
	private ArrayList<Proposal> myProposals; // list of proposals made by this replica
	private ArrayList<Proposal> receivedProposals; // list of proposals received as leader
	
	// Leader proposer variable
	private ArrayList<RespondNewLeaderMessage> respondElectionList;
	
	// Phase1 list of received prepare responses
	private ArrayList<PrepareResponseMessage> prepResponseList;
	// Phase2 list of received accepted notifications
	// Acceptor Variables
	private ArrayList<AcceptNotificationMessage> acceptNotificationList;
	// Learner variables
	private ArrayList<Proposal> learnedProposals;
	
	private ArrayList<NodeLocationData> locationDataList;
	private NodeLocationData locationData;
	private Message message;
	private ServerListener serverListener;
	
	private ReplicaHeartBeat heartbeat;
	private Map<Integer, HeartbeatListener> heartbeatListeners;
	
	public Replica(String host, int port, int id){
		this.id = id;
		this.ballotNum = new Pair(id);
		this.acceptNum = new Pair(id);
		this.acceptVal = 0;

		account = new Account();

		log = new Log(id);
		account.performOperations(log);
		
		isAlive = true;
		hasMajorityBallot = false;
		
		locationData = new NodeLocationData(host, port, id);
		serverListener = new ServerListener(this);
		serverListener.start();
		
		createLocationDataList();
		
		learnedProposals = new ArrayList<Proposal>();
		notAcceptedProposals = new ArrayList<Proposal>();
		myProposals = new ArrayList<Proposal>();
		receivedProposals = new ArrayList<Proposal>();
		prepResponseList = new ArrayList<PrepareResponseMessage>();
		acceptNotificationList = new ArrayList<AcceptNotificationMessage>();
		respondElectionList = new ArrayList<RespondNewLeaderMessage>();
		
		updateLeader(0);
		
		startThreads();
	}
	
	private void createLocationDataList() {
		locationDataList = new ArrayList<NodeLocationData>();
		for(int i = 0; i < nrOfReplicas; i++) {
			String tempHost = Main.replicaIpList[i];
			int tempPort = 8001;
			NodeLocationData temp = new NodeLocationData(tempHost, tempPort, i);
			locationDataList.add(temp);
		}
	}
	
	private void startThreads() {
		heartbeat = new ReplicaHeartBeat(this);
		heartbeat.start();
		heartbeatListeners = new HashMap<Integer, HeartbeatListener>();
		
		for(int i = 0; i < nrOfReplicas; i++) {
			// Do not create listener for itself
			if(i == this.id) {
				continue;
			}
			NodeLocationData temp = locationDataList.get(i);
			HeartbeatListener x = new HeartbeatListener(this, temp);
			x.start();
			heartbeatListeners.put(i, x);
		}
	}

	public void setListener(ReplicaListener listener){
		this.listener = listener;	
	}
	public NodeLocationData getLocationData(){
		return locationData;
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
		Proposal proposal = new Proposal(this.id, myProposals.size(), value, Type.DEPOSIT);
		myProposals.add(proposal);
		ProposeToLeaderMessage proposeMsg = new ProposeToLeaderMessage(proposal);
		proposeMsg.setSender(this.locationData);
		unicast(getCurrentLeader(), proposeMsg);
	}
	
	public void withdraw(double value){
		if(account.getBalance() < value) {return;}
		Proposal proposal = new Proposal(this.id, myProposals.size(), value, Type.WITHDRAW);
		myProposals.add(proposal);
		ProposeToLeaderMessage proposeMsg = new ProposeToLeaderMessage(proposal);
		proposeMsg.setSender(this.locationData);
		unicast(getCurrentLeader(), proposeMsg);
	}
	
	public void balance(){
		fireActionPerformed(Type.BALANCE, Status.SUCCESS);
	}
	
	public void fail(){
		if(this.isAlive) {
			this.isAlive = false;
			fireActionPerformed(Type.FAIL, Status.SUCCESS);
			System.out.println("---------------------------------------");
			System.out.println(this.id + ": HAS FAILED-----------------");
		}
	}

	public void unfail(){;
		if(!this.isAlive) {
			this.isAlive = true;
			
			account = new Account();
			log = new Log(id);
			account.performOperations(log);
			
			for(int i = 0; i < nrOfReplicas; i++) {
				if (!(i == this.id)){
					heartbeatListeners.get(i).interrupt();
				}
				else {continue;}
			}
			createLocationDataList();
			startThreads();
			requestNewLeaderId();
			
			System.out.println(this.heartbeatListeners.get(1));
			fireActionPerformed(Type.UNFAIL, Status.SUCCESS);
			System.out.println("---------------------------------------");
			System.out.println(this.id + ": IS UNFAILED-----------------");
		}
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
	public void updateLeader(int num) {
		if(locationData.getNum() == num) {
			locationData.becomeLeader();
		}
		// Update list of replicas accordingly
		for (int i = 0; i < nrOfReplicas; i++) {
			NodeLocationData tempLocationData = locationDataList.get(i);
			if(i != num) {
				tempLocationData.becomeNonLeader();
			}
			else {
				tempLocationData.becomeLeader();
			}
		}
		// Remove all responses and received proposals, since we are starting a new phase1
		prepResponseList.clear();
		receivedProposals.clear();
		hasMajorityBallot = false;
	}

	private synchronized void deliver(Message m) {
		if(!isAlive) {
			return;
		}
		else if(m instanceof HeartbeatMessage) {
//			System.out.println(this.id + ": HeartbeatMessage received at replica from: " + m.getSender().getNum());
			heartbeatListeners.get(m.getSender().getNum()).resetTimeout();
		}
		else if(m instanceof ProposeToLeaderMessage) {
			// TODO: Should not run phase 1 when leader has not changed, so proposeToLeaderMessage should else go to Phase2
			System.out.println("------------------------------------------------------");
			System.out.println(this.id + ": ProposeToLeaderMessage received at replica");
			
			ProposeToLeaderMessage proposeMessage = (ProposeToLeaderMessage)m;
			// When first proposal is received, leader starts phase1
			if(locationData.isLeader() && receivedProposals.isEmpty()) { 
				receivedProposals.add(proposeMessage.getProposal());
				Pair newBallot = new Pair(this.id);
				newBallot.setBallotNum(this.ballotNum.getBallotNum() + 1);
				newBallot.setId(this.id);
				PrepareRequestMessage prepareRequest = new PrepareRequestMessage(newBallot);
				prepareRequest.setSender(this.locationData);
				broadcast(prepareRequest);
			}
			// If majority has joined ballot, latter proposals lead straight to phase2
			else if(locationData.isLeader()) {
				while(!hasMajorityBallot) {
				}
				// Store all received transaction-proposals in a Map
				receivedProposals.add(proposeMessage.getProposal());
				AcceptNotificationMessage acceptReqMessage = new AcceptNotificationMessage(this.ballotNum, proposeMessage.getProposal());
				acceptReqMessage.setSender(this.locationData);
				broadcast(acceptReqMessage);

			}
			// Send it to all Replicas, because this is not the leader
			else {broadcast(m);}
		}
		else if(m instanceof PrepareRequestMessage) {
			System.out.println(this.id + ": PrepareRequestMessage received at replica");
			
			PrepareRequestMessage prepareRequest = (PrepareRequestMessage)m;
			Pair bal = prepareRequest.getBallotNum();
			if (isBallotBiggerOrEqual(bal, this.ballotNum)) {
				this.ballotNum = bal;
				PrepareResponseMessage prepareResponse = new PrepareResponseMessage(this.ballotNum, this.acceptNum, this.acceptVal);
				prepareResponse.setSender(this.locationData);
				unicast(prepareRequest.getSender(), prepareResponse);
			}
			else {return;}
		}

		else if(m instanceof PrepareResponseMessage) {
			System.out.println(this.id + ": PrepareResponseMessage received at replica");
			
			PrepareResponseMessage prepareResponse = (PrepareResponseMessage)m;
			prepResponseList.add(prepareResponse);
			
			// Check if response has come from a majority
			if(prepResponseList.size() == ((nrOfReplicas / 2) + 1)) {
				hasMajorityBallot = true;
				
				double highestVal = 0;
//				double myVal;
//				PrepareResponseMessage highestResponse = null;
				for (PrepareResponseMessage response : prepResponseList) {
					double tempVal = response.getAcceptVal();
					if(tempVal > highestVal) {
						highestVal = tempVal;
					}
//					if(isBallotBigger(response.getAcceptNum(), highestResponse.getBallotNum())){
//						highestResponse = response;
//					}
				}
				AcceptNotificationMessage acceptReqMessage;
				if(highestVal == 0) {
//					myVal = receivedProposals.get(0).getValue();
					acceptReqMessage = new AcceptNotificationMessage(prepareResponse.getBallotNum(), receivedProposals.get(0));
					} // TODO: find a way to store init proposed value
				else {
//					myVal = highestResponse.getAcceptVal();
//					acceptReqMessage = new AcceptNotificationMessage(prepareResponse.getBallotNum(), myVal);
					acceptReqMessage = new AcceptNotificationMessage(prepareResponse.getBallotNum(), receivedProposals.get(0));
				}
				acceptReqMessage.setSender(this.locationData);
				broadcast(acceptReqMessage);				
			}
			else {return;}
		}

		else if(m instanceof AcceptNotificationMessage) {
			
			AcceptNotificationMessage acceptNot = (AcceptNotificationMessage)m;
			Pair bal = acceptNot.getBallotNum();
			// If it is the first time it receives the acceptMsg(it's sent from the leader)
			if(!receivedAcceptNot(acceptNot)) {
				System.out.println(this.id + ": 1.st acceptNotificationMessage received at replica");
				
				if(isBallotBiggerOrEqual(bal, this.ballotNum)) {
					acceptNotificationList.add(acceptNot);
					
					this.acceptNum = bal;
					this.acceptVal = acceptNot.getProposal().getValue();
					AcceptNotificationMessage acceptNotification = new AcceptNotificationMessage(acceptNot.getBallotNum(), acceptNot.getProposal());
					acceptNotification.setSender(this.locationData);
					broadcast(acceptNotification);
				}
				// If the proposal is not accepted, notify the proposer Replica
				else {
					NotAcceptedNotificationMessage notAcceptedNot = new NotAcceptedNotificationMessage(acceptNot.getProposal());
					notAcceptedNot.setSender(this.locationData);
					int proposerId = acceptNot.getProposal().getProposerId();
					NodeLocationData proposerData = locationDataList.get(proposerId);
					unicast(proposerData, notAcceptedNot);
				}
			}
			else if(receivedAcceptNot(acceptNot)){
				System.out.println(this.id + ": acceptNotificationMessage received at replica");
				
				acceptNotificationList.add(acceptNot);
				// Check if notification has come from a majority
				if(receivedExtMajorityAcceptors(acceptNot) ) {
					// Decide value
					DecideMessage decideMsg = new DecideMessage(acceptNot.getProposal(), this.log.getLogList());
					decideMsg.setSender(this.locationData);
					Thread periodicThread = new PeriodicBroadcast(this, decideMsg);
				    periodicThread.start();
				}
				else {return;}
			}
		}
		
		else if(m instanceof DecideMessage){
//			System.out.println(this.id + ": DecideMessage received at replica");
			DecideMessage decideMsg = (DecideMessage)m;
			Proposal proposal = decideMsg.getProposal();
			// Only learn value if it has not already
			if(!receivedDecideMsg(decideMsg)) {
				if(this.log.size() < decideMsg.getLog().size()) {
					ArrayList<LogEntry> newEntries = getNewEntries(this.log.getLogList(), decideMsg.getLog());
					for (LogEntry logEntry : newEntries) {
						System.out.println(logEntry.getOperation() + " Op/Val "  + logEntry.getValue());
						Proposal restoredProposal = logEntry.getProposal();
						learnedProposals.add(restoredProposal);
						decideNew(logEntry.getOperation(), logEntry.getValue(), restoredProposal);
					}
				}
				else {
					learnedProposals.add(proposal);
					decide(proposal);
				}
			}
		}
		// Sends a fail message to CLI, if a majority has not accepted(rejected) the proposal 
		else if(m instanceof NotAcceptedNotificationMessage) {
			System.out.println(this.id + ": NotAcceptedNotficationMessage received at replica");
			
			NotAcceptedNotificationMessage notAcceptedNot = (NotAcceptedNotificationMessage)m;
			notAcceptedProposals.add(notAcceptedNot.getProposal());
			
			if(receivedExtMajorityNotAcceptors(notAcceptedNot) ) {
				Proposal proposal = notAcceptedNot.getProposal();
				if(proposal.getOperation() == Type.DEPOSIT) {
					account.withdraw(proposal.getValue());
					if(proposal.getProposerId() == this.id) {
						fireActionPerformed(Type.DEPOSIT, Status.FAIL);
					}
				}
				// Perform transaction deposit
				else if(proposal.getOperation() == Type.WITHDRAW) {
					account.deposit(proposal.getValue());
					if(proposal.getProposerId() == this.id) {
						fireActionPerformed(Type.WITHDRAW, Status.FAIL);
						// TODO: Must handle
					}
				}
			}
		}
		else if(m instanceof ProposeNewLeaderMessage) {
			System.out.println(this.id + ": ProposeNewLeaderMessage received at replica from replica: " + m.getSender().getNum());
			
			ProposeNewLeaderMessage proposeMsg = (ProposeNewLeaderMessage)m;
			// If the Id of the possible new leader is lower than this' Id it is accepted
			RespondNewLeaderMessage respondMsg;
			if(proposeMsg.getNum() <= this.id) {
				respondMsg = new RespondNewLeaderMessage(proposeMsg.getNum(), true);
			}
			// If it is higher, the proposal of leader is not accepted
			else if(proposeMsg.getNum() > this.id) {
				respondMsg = new RespondNewLeaderMessage(proposeMsg.getNum(), false);
			}
			else{return;}
			respondMsg.setSender(this.getLocationData());
			unicast(proposeMsg.getSender(), respondMsg);
		}
		
		else if(m instanceof RespondNewLeaderMessage) {
			System.out.println(this.id + ": RespondNewLeaderMessage received at replica");
			
			RespondNewLeaderMessage respondMsg = (RespondNewLeaderMessage)m;
			if(respondMsg.isAccepted()) {
				respondElectionList.add(respondMsg);
			}
			if(respondElectionList.size() == ((nrOfReplicas/2) + 1)) {
				System.out.println(this.id + ": has been elected as new LEADER!");
				NewLeaderNotificationMessage newLeaderNot = new NewLeaderNotificationMessage(respondMsg.getNum());
				newLeaderNot.setSender(this.getLocationData());
				broadcast(newLeaderNot);
			}
		}
		// New leader has been elected
		else if(m instanceof NewLeaderNotificationMessage) {
			System.out.println(this.id + ": NewLeaderNotificationMessage received at replica");
			NewLeaderNotificationMessage newLeaderNotification = (NewLeaderNotificationMessage)m;
			int newLeaderNum = newLeaderNotification.getNum();
			
			if(locationData.getNum() == newLeaderNum) {
				locationData.becomeLeader();
			}
			updateLeader(newLeaderNum);
		}
		else if(m instanceof RequestLeaderInfoMessage) {
			int currentLeaderId = getCurrentLeader().getNum();
			RespondLeaderInfoMessage leaderInfoMsg = new RespondLeaderInfoMessage(currentLeaderId);
			leaderInfoMsg.setSender(this.locationData);
			unicast(m.getSender(), leaderInfoMsg);
		}
		else if(m instanceof RespondLeaderInfoMessage) {
			RespondLeaderInfoMessage leaderInfoMsg = (RespondLeaderInfoMessage)m;
			int newLeaderId = leaderInfoMsg.getNum();
			if(locationData.getNum() == newLeaderId) {
				locationData.becomeLeader();
			}
			updateLeader(newLeaderId);
		}
		else {return;}
	}
	/*
	 * decide should update local log, replica and status of transaction in CLI
	 * @Param value - value that Paxos Algorithm decided
	 */
	private void decide(Proposal proposal) {
		System.out.println(this.id + ": Value: " + proposal.getValue() + " has been learned");
		System.out.println("------------------------------------------------------");
		
		double value = proposal.getValue();
		// Perform transaction withdrawal
		if(proposal.getOperation() == Type.WITHDRAW) {
			boolean success = false;
			try {
				account.withdraw(value);
				log.addEntry(new LogEntry(Type.WITHDRAW, value, proposal));
				success = true;
			} catch (IllegalArgumentException e){}
			
			if(proposal.getProposerId() == this.id) {
				fireActionPerformed(Type.WITHDRAW, success ? Status.SUCCESS : Status.FAIL);
			}
		}
		// Perform transaction deposit
		else if(proposal.getOperation() == Type.DEPOSIT) {
			account.deposit(value);
			log.addEntry(new LogEntry(Type.DEPOSIT, value, proposal));

			if(proposal.getProposerId() == this.id) {
				fireActionPerformed(Type.DEPOSIT, Status.SUCCESS);
			}
		}
	}
	private void decideNew(Type type, double value, Proposal proposal) {
		System.out.println(this.id + ": Value: " + value + " has been learned that was missed during failure");
		System.out.println("------------------------------------------------------");
		
		// Perform transaction withdrawal
		if(type == Type.WITHDRAW) {
			account.withdraw(value);
			log.addEntry(new LogEntry(type, value, proposal));
		}
		// Perform transaction deposit
		else if(type == Type.DEPOSIT) {
			account.deposit(value);
			log.addEntry(new LogEntry(type, value, proposal));
		}
	}
	/* 
	 * Send message to one receiver
	 * @Param m - Message to be sent
	 * @Param nodeLocationData - Data of location of the node to receive message 
	 */
	private void unicast(NodeLocationData nodeLocationData, Message m) {
		if(!isAlive) {return;}
		if(m instanceof HeartbeatMessage) {
			System.out.println("Heartbeat sent from replica: " + m.getSender() + "to replica " + nodeLocationData.getNum());
		}
		Communication.sendMessage(nodeLocationData, m);
	}
	void broadcast(Message m) {
		if(!isAlive) {return;}
		
		m.setSender(this.locationData);
		
		for(NodeLocationData locationData : locationDataList)
		{
			if(this.locationData.isEqualTo(locationData) && (m instanceof HeartbeatMessage || m instanceof RequestLeaderInfoMessage)) {
				continue;
			}
			// immediately deliver to self, but not if DecideMessage, because value has already been decided locally
			else if(this.locationData.isEqualTo(locationData)) {
				deliver(m);
			}
			// send message
			else {
				unicast(locationData, m);
			}
		}
	}
	/*
	 * Return true if ballot 1 is higher than ballot 2
	 */
	private boolean isBallotBiggerOrEqual(Pair bal1, Pair bal2) {
		if(bal1.getBallotNum() > bal2.getBallotNum()) {
			return true;
		}
		else if(bal1.getBallotNum() == bal2.getBallotNum() && bal1.getId() > bal2.getId()){
			return true;
		}
		else if(bal1.getBallotNum() == bal2.getBallotNum() && bal1.getId() == bal2.getId()) {
			return true;
		}
		return false;
	}
	// Check to see if acceptor has received acceptNots from exactly the # of majority
	private boolean receivedExtMajorityAcceptors(AcceptNotificationMessage acceptNot){
		int counter = 0;
		for(int i = 0; i < acceptNotificationList.size(); i++){
			if(acceptNotificationList.get(i).isEqual(acceptNot)) {
				counter++;
			}
		}
		if (counter == ((nrOfReplicas / 2)) + 1) {
			return true;
		}
		else {return false;}
	}
	
	// Check to see if proposer has received NotAcceptNots from exactly the # of majority
	private boolean receivedExtMajorityNotAcceptors(NotAcceptedNotificationMessage notAcceptNot){
		int counter = 0;
		for(int i = 0; i < notAcceptedProposals.size(); i++){
			if(notAcceptedProposals.get(i).isEqual(notAcceptNot.getProposal())) {
				counter++;
			}
		}
		if (counter == ((nrOfReplicas / 2)) + 1) {
			return true;
		}
		else {return false;}
	}
	
	private NodeLocationData getCurrentLeader(){
		for (int i = 0; i < nrOfReplicas; i++) {
			NodeLocationData tempLocationData = locationDataList.get(i);
			if(tempLocationData.isLeader()) {
				return tempLocationData;
			}
		}
		return null;
	}
	
	// Contacts other replicas to find out who is current leader
	private void requestNewLeaderId() {
		RequestLeaderInfoMessage leaderInfoMsg = new RequestLeaderInfoMessage();
		leaderInfoMsg.setSender(this.locationData);
		broadcast(leaderInfoMsg);
	}
	
	// Check to see if acceptor has already accepted this proposal
	private boolean receivedAcceptNot(AcceptNotificationMessage acceptNot) {
		for(int i = 0; i < acceptNotificationList.size(); i++){
			if(acceptNotificationList.get(i).isEqual(acceptNot)) {
				return true;
			}
		}
		return false;
	}

	// Check to see if learner has already learned/decided this value from proposal
	private boolean receivedDecideMsg(DecideMessage msg){
		for(int i = 0; i < learnedProposals.size(); i++){
			if(learnedProposals.get(i).isEqual(msg.getProposal())) {
				return true;
			}
		}
		return false;
	}
	
	private ArrayList<LogEntry> getNewEntries(ArrayList<LogEntry> oldList, ArrayList<LogEntry> newList) {
		ArrayList<LogEntry> newEntries = new ArrayList<LogEntry>();
		for(int i = oldList.size(); i < newList.size(); i++) {
			newEntries.add(newList.get(i));
		}
		return newEntries;
	}
	
	// Elect a new leader
	void electNewLeader() {
		if(!isAlive)
			return;
		// If this is the replica with lowest id, it immediately elect itself
//		if(this.id == 0) {
//			System.out.println(this.id + ": has been elected as new LEADER!");
//			NewLeaderNotificationMessage newLeaderNot = new NewLeaderNotificationMessage(this.id);
//			newLeaderNot.setSender(this.getLocationData());
//			broadcast(newLeaderNot);
//		}
		// Otherwise propose to all nodes that this Replica should be new leader
//		else {
			// Clear previous list of responses to a new election
			respondElectionList.clear();
			ProposeNewLeaderMessage newLeaderProposal = new ProposeNewLeaderMessage(this.id);
			newLeaderProposal.setSender(this.getLocationData());
			broadcast(newLeaderProposal);
//		}
	}
	
}