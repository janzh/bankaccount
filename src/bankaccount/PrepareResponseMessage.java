package bankaccount;

public class PrepareResponseMessage extends Message {
	private Pair ballotNum;
	private Pair acceptNum;
	private int acceptVal;
	
	public PrepareResponseMessage(Pair ballotNum, Pair acceptNum, int acceptVal) {;
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

	public int getAcceptVal() {
		return acceptVal;
	}
}
