package bankaccount;

public class ReplicaEvent {
	public enum Type { DEPOSIT, WITHDRAW, BALANCE, FAIL, UNFAIL };
	public enum Status { SUCCESS, FAIL };
	
	private Type type;
	private Status status;
	private double value;
	
	public ReplicaEvent(Type type, Status status, double value) { 
		this.type = type;
		this.status = status;
		this.value = value;
	}

	public Type getType() {
		return type;
	}

	public Status getStatus() {
		return status;
	}
	
	public double getValue(){
		return value;
	}
}
