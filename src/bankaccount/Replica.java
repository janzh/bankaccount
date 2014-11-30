package bankaccount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import bankaccount.ReplicaEvent.Status;
import bankaccount.ReplicaEvent.Type;

public class Replica {
	// TODO: Testing variables for fail/unfail commands
	private static final int heartbeatTimeout = 5000;
	private static final int heartbeatDelayMin = 1000;
	private static final int heartbeatDelayMax = 2000;
	
	private static final int nrOfReplicas = 3;
	private static int decideMsgPeriod = 5000; // Ms of each period between broadcasting decideMsg to all
	
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
	
	// Phase1 list of received prepare responses
	private ArrayList<PrepareResponseMessage> prepResponseList;
	// Phase2 list of received accepted notifications
	// Acceptor Variables
	private ArrayList<AcceptNotificationMessage> acceptNotificationList;
	// Learner variables
	private ArrayList<Proposal> learnedProposals;
	
	private ArrayList<Replica> replicas;
	private ArrayList<NodeLocationData> locationDataList;
	private NodeLocationData locationData;
	private Message message;
	private ServerListener serverListener;
	
	private ReplicaHeartbeat heartbeat;
	private Map<Integer, HeartbeatListener> heartbeatListeners;
	
	public Replica(String host, int port, int id){
		this.id = id;
		this.ballotNum = new Pair(id);
		this.acceptNum = new Pair(id);
		this.acceptVal = 0;

		account = new Account();

		log = new Log(id);
		account.performOperations(log);
		
		replicas = new ArrayList<Replica>();
		locationDataList = new ArrayList<NodeLocationData>();
		
		isAlive = true;
		hasMajorityBallot = false;
		
		locationData = new NodeLocationData(host, port, id);
		serverListener = new ServerListener(this);
		serverListener.start();
		
		heartbeat = new ReplicaHeartbeat();
		heartbeat.start();
		heartbeatListeners = new HashMap<Integer, HeartbeatListener>();
		
		for(int i = 0; i < nrOfReplicas; i++) {
			String tempHost = "localhost";
			int tempPort = 8001+i;
			NodeLocationData temp = new NodeLocationData(tempHost, tempPort, i);
			locationDataList.add(temp);
			if(i == this.id) {
				continue;
			}
			HeartbeatListener x = new HeartbeatListener(temp);
			x.start();
			heartbeatListeners.put(i, x);
		}
		
		learnedProposals = new ArrayList<Proposal>();
		notAcceptedProposals = new ArrayList<Proposal>();
		myProposals = new ArrayList<Proposal>();
		receivedProposals = new ArrayList<Proposal>();
		prepResponseList = new ArrayList<PrepareResponseMessage>();
		acceptNotificationList = new ArrayList<AcceptNotificationMessage>();
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
		Proposal proposal = new Proposal(this.id, myProposals.size(), value, "d");
		myProposals.add(proposal);
		ProposeToLeaderMessage proposeMsg = new ProposeToLeaderMessage(proposal);
		proposeMsg.setSender(this.locationData);
		unicast(getCurrentLeader().getLocationData(), proposeMsg);
		
	}
	public void withdraw(double value){
		Proposal proposal = new Proposal(this.id, myProposals.size(), value, "w");
		myProposals.add(proposal);
		ProposeToLeaderMessage proposeMsg = new ProposeToLeaderMessage(proposal);
		proposeMsg.setSender(this.locationData);
		unicast(getCurrentLeader().getLocationData(), proposeMsg);

	}
	public void balance(){
		fireActionPerformed(Type.BALANCE, Status.SUCCESS);
	}
	public void fail(){
		this.isAlive = false;
		this.heartbeat.kill();
		fireActionPerformed(Type.FAIL, Status.SUCCESS);
		System.out.println("---------------------------------------");
		System.out.println(this.id + ": HAS FAILED-----------------");
	}

	public void unfail(){
		this.isAlive = true;
		this.heartbeat.wakeUp();
		fireActionPerformed(Type.UNFAIL, Status.SUCCESS);
		System.out.println("---------------------------------------");
		System.out.println(this.id + ": IS UNFAILED-----------------");
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
		// Update list of replicas accordingly
		for (int i = 0; i < replicas.size(); i++) {
			Replica tempReplica = replicas.get(i);
			NodeLocationData tempLocationData = locationDataList.get(i);
			if(i != num) {
				tempReplica.getLocationData().becomeNonLeader();
				tempLocationData.becomeNonLeader();
			}
			else {
				tempReplica.getLocationData().becomeLeader();
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
			if(prepResponseList.size() == ((replicas.size() / 2) + 1)) {
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
					NodeLocationData proposerData = replicas.get(proposerId).getLocationData();
					unicast(proposerData, notAcceptedNot);
				}
			}
			else if(receivedAcceptNot(acceptNot)){
				System.out.println(this.id + ": acceptNotificationMessage received at replica");
				
				acceptNotificationList.add(acceptNot);
				// Check if notification has come from a majority
				if(receivedExtMajorityAcceptors(acceptNot) ) {
					// Decide value
					DecideMessage decideMsg = new DecideMessage(acceptNot.getProposal());
					decideMsg.setSender(this.locationData);
					Thread periodicThread = new PeriodicBroadcast(decideMsg);
				    periodicThread.start();
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
		// Sends a fail message to CLI, if a majority has not accepted(rejected) the proposal 
		else if(m instanceof NotAcceptedNotificationMessage) {
			System.out.println(this.id + ": NotAcceptedNotficationMessage received at replica");
			
			NotAcceptedNotificationMessage notAcceptedNot = (NotAcceptedNotificationMessage)m;
			notAcceptedProposals.add(notAcceptedNot.getProposal());
			
			if(receivedExtMajorityNotAcceptors(notAcceptedNot) ) {
				Proposal proposal = notAcceptedNot.getProposal();
				if(proposal.getType().equals("w")) {
					account.withdraw(proposal.getValue());
					if(proposal.getProposerId() == this.id) {
						fireActionPerformed(Type.DEPOSIT, Status.FAIL);
						// TODO: Must handle
					}
				}
				// Perform transaction deposit
				else if(proposal.getType().equals("d")) {
					account.deposit(proposal.getValue());
					if(proposal.getProposerId() == this.id) {
						fireActionPerformed(Type.WITHDRAW, Status.FAIL);
					}
				}
			}
		}
		// New leader has been elected
		else if(m instanceof NewLeaderNotificationMessage) {
			NewLeaderNotificationMessage newLeaderNotification = (NewLeaderNotificationMessage)m;
			int newLeaderNum = newLeaderNotification.getNum();
			
			if(locationData.getNum() == newLeaderNum) {
				locationData.becomeLeader();
			}
			updateLeader(newLeaderNum);
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
		if(proposal.getType().equals("w")) {
			account.withdraw(value);
			log.addEntry(new LogEntry(Type.WITHDRAW, value));
			
			if(proposal.getProposerId() == this.id) {
				fireActionPerformed(Type.WITHDRAW, Status.SUCCESS);
				// TODO: Must handle
			}
		}
		// Perform transaction deposit
		else if(proposal.getType().equals("d")) {
			account.deposit(value);
			log.addEntry(new LogEntry(Type.DEPOSIT, value));

			if(proposal.getProposerId() == this.id) {
				fireActionPerformed(Type.DEPOSIT, Status.SUCCESS);
			}
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
		
		m.setSender(this.locationData);
		
		for(Replica replica : replicas)
		{
			if(this.locationData.isEqualTo(replica.getLocationData()) && m instanceof HeartbeatMessage) {
				continue;
			}
			// immediately deliver to self, but not if DecideMessage, because value has already been decide locally
			else if(this.locationData.isEqualTo(replica.getLocationData())) {
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
		if (counter == ((replicas.size() / 2)) + 1) {
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
	
	// Elect a new leader
	private void electNewLeader() {
		// TODO Auto-generated method stub
		System.out.println("NEW LEADER WAS ELECTED///----------------");
		if(!isAlive)
			return;
		int newNum = -1;
		
		// find old leader and calculate new leader number
		for(Replica replica: replicas) {
			NodeLocationData locationData = replica.getLocationData();
			if(locationData.isLeader()) {
				newNum = (locationData.getNum() + 1) % replicas.size();
				break;
			}
		}
		NewLeaderNotificationMessage newLeaderNot = new NewLeaderNotificationMessage(newNum);
		newLeaderNot.setSender(this.getLocationData());
		broadcast(newLeaderNot);
	}
	
	// Periodically sends (DecideMessage) to all replicas each
	public class PeriodicBroadcast extends Thread{
		private DecideMessage decideMsg;
		
		public PeriodicBroadcast(DecideMessage decideMsg) {
			this.decideMsg = decideMsg; 
		}
		@Override
	    public void run()
	    {
	        while(true) {
	           broadcast(decideMsg);
	           try {
				Thread.sleep(Replica.decideMsgPeriod);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        }
	    }
	}
	
	public class ReplicaHeartbeat extends Thread {
		private boolean isAlive;
		private long lastHeartbeat;
		private Random rand;
		
		public ReplicaHeartbeat() {
			isAlive = true;
			lastHeartbeat = System.currentTimeMillis();
			rand = new Random();
		}

		public void run() {
			int heartbeatDelay = rand.nextInt(heartbeatDelayMax - heartbeatDelayMin) + heartbeatDelayMin;
			while(isAlive) {
				if(heartbeatDelay < System.currentTimeMillis() - lastHeartbeat) {
					HeartbeatMessage heartbeatMessage = new HeartbeatMessage();
					broadcast(heartbeatMessage);
					lastHeartbeat = System.currentTimeMillis();
					heartbeatDelay = rand.nextInt(heartbeatDelayMax - heartbeatDelayMin) + heartbeatDelayMin;
				}
				yield(); // so the while loop doesn't spin too much
			}
		}
		public void kill() {
			isAlive = false;
		}
		
		public void wakeUp() {
			isAlive = true;
		}
	}

	public class HeartbeatListener extends Thread {
		private boolean isAlive;
		private long lastHeartbeat;
		private NodeLocationData locationData;
		
		public HeartbeatListener(NodeLocationData locationData) {
			this.isAlive = true;
			this.lastHeartbeat = System.currentTimeMillis();
			this.locationData = locationData;
		}
		
		public void resetTimeout() {
			lastHeartbeat = System.currentTimeMillis();
		}

		public void run() {
			while(isAlive) {
				if(heartbeatTimeout < System.currentTimeMillis() - lastHeartbeat){
					// if was leader, elect a new one
					if(locationData.isLeader())
						electNewLeader();

					lastHeartbeat = System.currentTimeMillis();
				}
				yield(); // so the while loop doesn't spin too much
			}
		}
		
		public void kill(){
			isAlive = false;
		}
	}
}
