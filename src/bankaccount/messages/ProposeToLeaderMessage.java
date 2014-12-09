package bankaccount.messages;

import bankmanager.Proposal;

public class ProposeToLeaderMessage extends Message {
	private Proposal proposal;
	
	public ProposeToLeaderMessage(Proposal proposal) {
		this.proposal = proposal;
	}

	public Proposal getProposal() {
		return proposal;
	}
}
