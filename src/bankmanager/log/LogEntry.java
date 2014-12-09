package bankmanager.log;

import java.io.Serializable;

import bankmanager.Proposal;
import bankmanager.ReplicaEvent.Type;

public class LogEntry implements Serializable{
	private Type operation;
	private double value;
	private Proposal proposal;
	
	public LogEntry(Type operation, double value, Proposal proposal){
		this.operation = operation;
		this.value = value;
		this.proposal = proposal;
	}

	public Type getOperation() {
		return operation;
	}

	public double getValue() {
		return value;
	}
	
	public Proposal getProposal() {
		return proposal;
	}
}
