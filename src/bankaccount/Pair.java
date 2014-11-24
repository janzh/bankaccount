package bankaccount;

import java.io.Serializable;

public class Pair implements Serializable{
	private int id;
	private int ballotNum;
	
	public Pair(int id) {
		this.id = id;
	}
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getBallotNum() {
		return ballotNum;
	}

	public void setBallotNum(int ballotNum) {
		this.ballotNum = ballotNum;
	}
}
