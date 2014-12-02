package bankaccount.log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import bankaccount.ReplicaEvent.Type;

public class LogBackup {
	
	private String fileName = "stableStorage/backup.csv";
	private String fileLearnedName = "stableStorage/backup.csv";
	private static final String separator = ",";

	/*public static void main(String[] args) {
		LogBackup backup = new LogBackup();
		
		backup.read();
		
		backup.appendEntry(Type.DEPOSIT, 500);
		backup.appendEntry(Type.DEPOSIT, 23.5);
		backup.appendEntry(Type.WITHDRAW, 150);
		
		backup.read();
	}*/
	
	public LogBackup(){}
	
	public LogBackup(String fileName, String learnedFileName){
		this.fileName = "stableStorage/"+fileName+".csv";
		this.fileLearnedName = "stableStorage/"+learnedFileName+".csv";
	}

	public ArrayList<LogEntry> read() { 
		BufferedReader br = null;
		ArrayList<LogEntry> logEntries = new ArrayList<LogEntry>();
		try {
			String line;
			br = new BufferedReader(new FileReader(fileName));
			while ((line = br.readLine()) != null) {
				String[] entryStrings = line.split(separator);
				for(int i=0; i<entryStrings.length; i++){
					System.out.println((i+1)+": " + entryStrings[i]);
					logEntries.add(extractLogEntry(entryStrings[i]));
				}
			}
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
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
		if (entry.getOperation() == Type.DEPOSIT) return "d("+entry.getValue()+")"; 
		else if (entry.getOperation() == Type.WITHDRAW) return "w("+entry.getValue()+")";
		else return null;
	}
	
	private LogEntry extractLogEntry(String entryString){
		String[] substrings = entryString.split("\\(");
		String operation = substrings[0];
		double value = Double.parseDouble(substrings[1].split("\\)")[0]);
		if (operation.equals("d")){
//			return new LogEntry(Type.DEPOSIT, value);
		}
		if (operation.equals("w")){
//			return new LogEntry(Type.WITHDRAW, value);
		}
		return null;
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