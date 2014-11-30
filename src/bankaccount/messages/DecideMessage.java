package bankaccount.messages;

import bankaccount.Proposal;

public class DecideMessage extends Message {
	private Proposal proposal;
	
	public DecideMessage(Proposal proposal){
		this.proposal = proposal;
	}
	
	public Proposal getProposal(){
		return proposal;
	}
}
