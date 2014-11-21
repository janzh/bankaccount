package bankaccount;

import java.util.ArrayList;

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
	
	private ArrayList<Replica> replicas;
	private NodeLocationData locationData;
	private String message;
	private ServerListener serverListener;
	
	public Replica(ReplicaListener listener, String host, int port, int id){
		this.id = id;
		this.ballotNum = new Pair();
		this.acceptNum = new Pair();
		
		this.listener = listener;

		account = new Account();
		log = new Log();
		replicas = new ArrayList<>();
		isAlive = true;
		
		locationData = new NodeLocationData(host, port, id);
		serverListener = new ServerListener(this);
		serverListener.start();
	}
	
	public NodeLocationData getLocationData(){
		return locationData;
	}
	
	public void setReplicaList(ArrayList<Replica> replicas){
		this.replicas = replicas;
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
	
	private void fireActionPerformed(Type type, String message){
		if (listener != null) listener.replicaActionPerformed(new ReplicaEvent(type, message));
	}

	public void setMessage(String receivedMessage) {
		this.message = receivedMessage;
		fireActionPerformed(Type.RECEIVE, message);	
	}
}