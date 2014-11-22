package bankaccount;

import bankaccount.Pair;

public class AcceptNotificationMessage extends Message {
	private Pair ballotNum;
	private int value;
	
	public AcceptNotificationMessage(Pair ballotNum, int value) {
		this.ballotNum = ballotNum;
		this.value = value;
	}

	public Pair getBallotNum() {
		return ballotNum;
	}

	public int getValue() {
		return value;
	}
}

