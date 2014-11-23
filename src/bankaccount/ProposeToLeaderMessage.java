package bankaccount;

public class ProposeToLeaderMessage extends Message {
	private int id;
	private int value;
	
	public ProposeToLeaderMessage(int id, int value) {
		this.id = id;
		this.value = value;
	}

	public int getId() {
		return id;
	}

	public int getValue() {
		return value;
	}
}
