package bankaccount.messages;

import bankmanager.Pair;
import bankmanager.Proposal;

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
	
	public boolean isEqual(AcceptNotificationMessage msg2) {
		// If both pair of Proposal and Ballots are equal, the messages are equal
		if (this.getBallotNum().isEqual(msg2.getBallotNum()) && this.getProposal().isEqual(msg2.getProposal())) {
			return true;
		}
		return false;
	}
}

