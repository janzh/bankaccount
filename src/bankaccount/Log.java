package bankaccount;

import java.util.ArrayList;

public class Log {
	
	private ArrayList<LogEntry> log;
	private LogBackup logBackup;
	
	public Log(int id) {
		logBackup = new LogBackup("backup"+id);
		log = logBackup.read();
	}
	
	public LogEntry getEntry(int i){
		return log.get(i);
	}
	
	public void addEntry(LogEntry entry){
		log.add(entry);
		logBackup.appendEntry(entry);
	}
	
	public int size(){
		return log.size();
	}
}
