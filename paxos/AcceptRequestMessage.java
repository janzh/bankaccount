package paxos;

public class AcceptRequestMessage extends Message1
{
	private Proposal proposal;
	
	public AcceptRequestMessage(Proposal proposal)
	{
		this.proposal = proposal;
	}
	
	public Proposal getProposal()
	{
		return proposal;
	}
}
