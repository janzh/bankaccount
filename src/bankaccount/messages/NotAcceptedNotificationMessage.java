package bankaccount.messages;

import bankaccount.Proposal;

public class NotAcceptedNotificationMessage extends Message{
	private Proposal proposal;
	
	public NotAcceptedNotificationMessage(Proposal proposal) {
		this.proposal = proposal;
	}
	
	public Proposal getProposal() {
		return proposal;
	}
}
