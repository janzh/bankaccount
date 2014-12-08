package bankaccount;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import bankaccount.ReplicaEvent.Type;
import bankaccount.log.LogEntry;

public class CLI  implements ReplicaListener {
	private Replica replica;

	private String cmd;
	private double cmdValue;

	public CLI (Replica replica) {
		this.replica = replica;
		this.cmd = "";
		this.cmdValue = 0;
		
		try {
			readInput();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void print(String response) {
		System.out.println(response);
	}

	private void cmdFormat(String s) {
		String[] substrings = s.split("\\(");
		cmd = substrings[0];
		if(cmd.equals("balance")){
			return;
		}
		else if (cmd.equals("withdrawal") || cmd.equals("deposit")){
			substrings = substrings[1].split("\\)");
			cmdValue = Double.parseDouble(substrings[0]);
		}
		else {return;}
	}


	public void readInput() throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true){
			cmdFormat(br.readLine());
			if (cmd.equals("balance")) {
				replica.balance();
			}
			else if (cmd.equals("deposit")) {
				replica.deposit(cmdValue);
			}
			else if (cmd.equals("withdraw")){
				replica.withdraw(cmdValue);
			}
			else if (cmd.equals("fail")) {
				replica.fail();
			}
			else if (cmd.equals("unfail")) {
				replica.unfail();
			} else if (cmd.equals("print")) {
				ArrayList<LogEntry> logList = replica.getLog().getLogList();
				System.out.println("Log:");
				for (LogEntry entry : logList){
					System.out.println(entry.getOperation().toString()+": "+entry.getValue());
				}
			}
		}
	}

	@Override
	public void replicaActionPerformed(ReplicaEvent e) {
		if (e.getType() == Type.BALANCE){
			print("Balance: "+e.getValue());
		} else {
			print(e.getStatus().toString());
		}
	}
}