package bankmanager.messages;

import java.util.ArrayList;

import bankmanager.Proposal;
import bankmanager.log.LogEntry;

public class DecideMessage extends Message {
	private Proposal proposal;
	private ArrayList<LogEntry> log;
	
	public DecideMessage(Proposal proposal, ArrayList<LogEntry> log){
		this.proposal = proposal;
		this.log = log;
	}

	public Proposal getProposal(){
		return proposal;
	}
	
	public ArrayList<LogEntry> getLog() {
		return log;
	}
}
