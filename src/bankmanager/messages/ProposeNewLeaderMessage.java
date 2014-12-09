package bankmanager.messages;

public class ProposeNewLeaderMessage extends Message {
	private int num;
	
	public ProposeNewLeaderMessage(int num) {
		this.num = num;
	}
	
	public int getNum() {
		return num;
	}
}
