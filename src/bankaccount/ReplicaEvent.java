package bankaccount;

import bankaccount.messages.Message;

public class ReplicaEvent {
	public enum Type { RECEIVE, DEPOSIT, WITHDRAW, BALANCE, FAIL, UNFAIL, PRINT };
	public enum Status { SUCCESS, FAIL };
	
	private Type type;
	private Status status;
	private double value;
	private Message message;
	
	public ReplicaEvent(Type type, Status status, double value) { 
		this.type = type;
		this.status = status;
		this.value = value;
	}

	public ReplicaEvent(Type type, Message message) {
		this.type = type;
		this.message = message;
		this.status = Status.SUCCESS;
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
	
	public Message getMessage(){
		return message;
	}
}
