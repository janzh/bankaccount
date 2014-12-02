package bankaccount;

import java.io.Serializable;

import bankaccount.ReplicaEvent.Type;

public class Proposal implements Serializable{
	
	private int proposerId;
	private int proposalNum;
	private double value;
	private Type operation; // "w" or "d" <--> "withdraw" or "deposit"
	
	public Proposal(int proposerId, int proposalNum, double value, Type operation) {
		this.proposerId = proposerId;
		this.proposalNum = proposalNum;
		this.value = value;
		this.operation = operation;
	}
	
	public double getValue() {
		return value;
	}
	
	public int getProposalNum() {
		return proposalNum;
	}

	public int getProposerId() {
		return proposerId;
	}
	
	public Type getOperation() {
		return operation;
	}
	
	public boolean isEqual(Proposal p) {
		if(this.proposerId == p.proposerId && this.proposalNum == p.getProposalNum() && this.value == p.getValue()) {
			return true;
		}
		return false;
	}
}
