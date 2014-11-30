package bankaccount.messages;

public class RespondLeaderInfoMessage extends Message {
	private int num;
	
	public RespondLeaderInfoMessage(int num) {
		this.num = num;
	}
	
	public int getNum() {
		return num;
	}
}
