package bankaccount;

import bankaccount.ReplicaEvent.Status;
import bankaccount.ReplicaEvent.Type;

public class Replica {
	Account account;
	Log log;
	ReplicaListener listener;
	boolean isAlive;
	
	public Replica(ReplicaListener listener){
		this.listener = listener;
		isAlive = true;
	}
	
	public boolean isAlive(){
		return isAlive;
	}
	
	public void deposit(double value){
		fireActionPerformed(Type.DEPOSIT, Status.SUCCESS);
	}
	
	public void withdraw(double value){
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
		if (listener != null) listener.replicaActionPerformed(new ReplicaEvent(type, status));
	}
}