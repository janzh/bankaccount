package bankaccount;

public class ReplicaEvent {
	public enum Type { DEPOSIT, WITHDRAW, BALANCE, FAIL, UNFAIL };
	public enum Status { SUCCESS, FAIL };
	
	private Type type;
	private Status status;
	
	public ReplicaEvent(Type type, Status status) { 
		this.type = type;
		this.status = status;
	}

	public Type getType() {
		return type;
	}

	public Status getStatus() {
		return status;
	}
}
