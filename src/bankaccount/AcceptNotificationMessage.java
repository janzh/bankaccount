package bankaccount;

import bankaccount.Pair;

public class AcceptNotificationMessage extends Message {
	private Pair ballotNum;
	private double value;
	
	public AcceptNotificationMessage(Pair ballotNum, double value) {
		this.ballotNum = ballotNum;
		this.value = value;
	}

	public Pair getBallotNum() {
		return ballotNum;
	}

	public double getValue() {
		return value;
	}
}

