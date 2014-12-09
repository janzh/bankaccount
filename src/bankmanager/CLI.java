package bankmanager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import bankmanager.ReplicaEvent.Type;
import bankmanager.log.LogEntry;

public class CLI  implements ReplicaListener {
	private Replica replica;

	private String cmd;
	private double cmdValue;

	public CLI (Replica replica) {
		this.replica = replica;
		this.cmd = "";
		this.cmdValue = 0;
	}
	public void println(String response) {
		System.out.println(response);
	}

	private void cmdFormat(String s) {
		String[] substrings = s.split("\\(");
		cmd = substrings[0];
		if (cmd.equals("withdraw") || cmd.equals("deposit")){
			substrings = substrings[1].split("\\)");
			cmdValue = Double.parseDouble(substrings[0]);
		}
		else return;
	}

	public void readInput() throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line;
		while ((line = br.readLine()) != null){
			cmdFormat(line);
			if(cmd.equals("balance")) {
				replica.balance();
			}
			else if(cmd.equals("deposit")) {
				replica.deposit(cmdValue);
			}
			else if(cmd.equals("withdraw")) {
				replica.withdraw(cmdValue);
			}
			else if(cmd.equals("fail")) {
				replica.fail();
			}
			else if(cmd.equals("unfail")) {
				replica.unfail();
			}
			else if(cmd.equals("print")) {
				printLog();
			}
		}
	}

	@Override
	public void replicaActionPerformed(ReplicaEvent e) {
		if (e.getType() == Type.BALANCE){
			println("Balance: "+e.getValue());
		}
		if (e.getType() == Type.DEPOSIT || e.getType() == Type.WITHDRAW){
			println("Status: "+e.getStatus().toString());	
		}
	}

	private void printLog(){
		ArrayList<LogEntry> logList = replica.getLog().getLogList();
		println("Log:");
		if (logList.isEmpty()) println("Empty!");
		for (LogEntry entry : logList){
			println(entry.getOperation().toString()+": "+entry.getValue());
		}
	}

}