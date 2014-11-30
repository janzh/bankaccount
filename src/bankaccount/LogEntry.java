package bankaccount;

import bankaccount.ReplicaEvent.Type;

public class LogEntry {
	private Type operation;
	private double value;
	
	public LogEntry(Type operation, double value){
		this.operation = operation;
		this.value = value;
	}

	public Type getOperation() {
		return operation;
	}

	public double getValue() {
		return value;
	}
}
