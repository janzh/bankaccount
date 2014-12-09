package bankmanager;

import java.io.IOException;

public class LocalMain {
	
	public static void main(String[] args){
		Replica.local = true;
		int id = Integer.parseInt(args[0]);
		Replica r = new Replica("localhost", 8001+id, id);

		CLI cli = new CLI(r);
		r.setListener(cli);

		try {
			cli.readInput();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
