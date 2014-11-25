package bankaccount;

import java.io.Serializable;

public class Proposal implements Serializable{
	
	private int proposerId;
	private int proposalNum;
	private double value;
	private String type; // "w" or "d" <--> "withdraw" or "deposit"
	
	public Proposal(int proposerId, int proposalNum, double value, String type) {
		this.proposerId = proposerId;
		this.proposalNum = proposalNum;
		this.value = value;
		this.type = type;
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
	
	public String getType() {
		return type;
	}
	
	public boolean isEqual(Proposal p) {
		if(this.proposerId == p.proposerId && this.proposalNum == p.getProposalNum() && this.value == p.getValue()) {
			return true;
		}
		return false;
	}
}
