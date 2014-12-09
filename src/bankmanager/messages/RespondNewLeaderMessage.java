package bankmanager.messages;

public class RespondNewLeaderMessage extends Message {
	private int num;
	private boolean isAccepted;
	
	public RespondNewLeaderMessage(int num, boolean isAccepted) {
		this.num = num;
		this.isAccepted = isAccepted;
	}
	
	public int getNum() {
		return num;
	}
	
	public boolean isAccepted(){
		return isAccepted;
	}
}
