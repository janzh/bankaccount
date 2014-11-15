package bankaccount;

import bankaccount.ReplicaEvent.Status;
import bankaccount.ReplicaEvent.Type;

public class Replica {
	private Account account;
	private Log log;
	private ReplicaListener listener;
	private boolean isAlive;
	private int id;
	
	public Replica(ReplicaListener listener, int id){
		this.listener = listener;
		this.id = id;
		account = new Account();
		log = new Log();
		isAlive = true;
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
}