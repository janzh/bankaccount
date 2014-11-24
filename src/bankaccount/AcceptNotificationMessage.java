package bankaccount;

import bankaccount.Pair;

public class AcceptNotificationMessage extends Message {
	private Pair ballotNum;
	private Proposal proposal;
	private double value;
	
	public AcceptNotificationMessage(Pair ballotNum, double value) {
		this.ballotNum = ballotNum;
		this.value = value;
	}
	
	public AcceptNotificationMessage(Pair ballotNum, Proposal proposal) {
		this.ballotNum = ballotNum;
		this.proposal = proposal;
	}

	public Pair getBallotNum() {
		return ballotNum;
	}

	public Proposal getProposal() {
		return proposal;
	}

	public double getValue() {
		return value;
	}
}

