package bankaccount.log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import bankaccount.Proposal;
import bankaccount.ReplicaEvent.Type;

public class LogBackup {
	
	private String fileName = "stableStorage/backup.csv";
	private static final String separator = ",";
	
	public LogBackup(){}
	
	public LogBackup(String fileName){
		this.fileName = "stableStorage/"+fileName+".csv";
	}

	public ArrayList<LogEntry> read() { 
		BufferedReader br = null;
		ArrayList<LogEntry> logEntries = new ArrayList<LogEntry>();
		try {
			String line;
			br = new BufferedReader(new FileReader(fileName));
			while ((line = br.readLine()) != null) {
				String[] entryStrings = line.split(separator);
				for(int i=0; i < entryStrings.length; i++){
					System.out.println((i+1)+": " + entryStrings[i]);
					logEntries.add(extractLogEntry(entryStrings[i]));
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("A backup of the log does not exist.");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return logEntries;
	}
	
	public void appendEntry(LogEntry entry){
		appendEntry(parseLogEntry(entry) + separator);
	}
	
	private String parseLogEntry(LogEntry entry){
		int hostId = entry.getProposal().getProposerId();
		int propId = entry.getProposal().getProposalNum();
		if (entry.getOperation() == Type.DEPOSIT) return "d("+entry.getValue()+")"+hostId+"/"+propId; 
		else if (entry.getOperation() == Type.WITHDRAW) return "w("+entry.getValue()+")"+hostId+"/"+propId;
		else return null;
	}
	
	private LogEntry extractLogEntry(String entryString){
		Proposal tempProposal;
		String[] substrings1 = entryString.split("\\(");
		String operation = substrings1[0];
		String[] substrings2 = substrings1[1].split("\\)");
		double value = Double.parseDouble(substrings2[0]);
		String[] proposalValues = substrings2[1].split("\\/");
		int proposalId = Integer.parseInt(proposalValues[0]);
		int proposalNum = Integer.parseInt(proposalValues[1]);
		if (operation.equals("d")){
			tempProposal = new Proposal(proposalId, proposalNum, value, Type.DEPOSIT);
			return new LogEntry(Type.DEPOSIT, value, tempProposal);
		}
		else if (operation.equals("w")){
			tempProposal = new Proposal(proposalId, proposalNum, value, Type.WITHDRAW);
			return new LogEntry(Type.WITHDRAW, value, tempProposal);
		}
		else {return null;}
	}

	private void appendEntry(String entry){
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)))) {
			out.print(entry);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}