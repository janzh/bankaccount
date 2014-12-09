package bankmanager.messages;

import bankmanager.Pair;

public class PrepareResponseMessage extends Message {
	private Pair ballotNum;
	private Pair acceptNum;
	private double acceptVal;
	
	public PrepareResponseMessage(Pair ballotNum, Pair acceptNum, double acceptVal) {;
		this.ballotNum = ballotNum;
		this.acceptNum = acceptNum;
		this.acceptVal = acceptVal;
	}
	
	public Pair getBallotNum() {
		return ballotNum;
	}

	public Pair getAcceptNum() {
		return acceptNum;
	}

	public double getAcceptVal() {
		return acceptVal;
	}
}
