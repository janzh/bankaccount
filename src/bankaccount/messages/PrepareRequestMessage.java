package bankaccount.messages;

import bankaccount.Pair;

public class PrepareRequestMessage extends Message {
	private Pair ballotNum; 
	
	public PrepareRequestMessage(Pair ballotNum) {
		this.ballotNum = ballotNum;
	}
	
	public Pair getBallotNum() {
		return ballotNum;
	}
}
