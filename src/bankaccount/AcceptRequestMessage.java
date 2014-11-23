package bankaccount;

public class AcceptRequestMessage extends Message {
	private Pair ballotNum;
	private double value;
	
	public AcceptRequestMessage(Pair ballotNum, double value) {
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
