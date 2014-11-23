package paxos;

public class NewLeaderNotificationMessage extends Message1
{
	private int num;
	
	public NewLeaderNotificationMessage(int num)
	{
		this.num = num;
	}
	
	public int getNum()
	{
		return num;
	}
}
