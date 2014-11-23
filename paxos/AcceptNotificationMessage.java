package paxos;

public class AcceptNotificationMessage extends Message1
{
	private Proposal proposal;
	
	public AcceptNotificationMessage(Proposal proposal)
	{
		this.proposal = proposal;
	}
	
	public Proposal getProposal()
	{
		return proposal;
	}
}