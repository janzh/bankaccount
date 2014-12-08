package bankaccount;

import java.io.IOException;

public class Main {
	
	public static final String[] replicaIpList = {
		"54.67.92.43",
		"54.172.132.166",
		"54.72.172.236",
		"54.169.58.224",
		"54.94.205.169",
	};
	
	public static void main(String[] args){
		int id = Integer.parseInt(args[0]);
		Replica r = new Replica(replicaIpList[id], 8001, id);

		CLI cli = new CLI(r);
		r.setListener(cli);

		try {
			cli.readInput();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
