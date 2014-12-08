package bankaccount;

import java.io.IOException;

public class Main {
	public static void main(String[] args){
		Replica r = new Replica("54.148.157.54", 8001, 0);
		//Replica r = new Replica("54.69.100.172", 8001, 1);
		//Replica r = new Replica("54.149.12.69", 8001, 2);
		//Replica r = new Replica("54.69.199.175", 8001, 3);
		//Replica r = new Replica("54.148.74.72", 8001, 4);
		//r.updateLeader(0);
		CLI cli = new CLI(r);
		r.setListener(cli);
		
		try {
			cli.readInput();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
