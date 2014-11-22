package bankaccount;

public class AcceptRequestMessage extends Message {
	private Pair ballotNum;
	private int value;
	
	public AcceptRequestMessage(Pair ballotNum, int value) {
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
