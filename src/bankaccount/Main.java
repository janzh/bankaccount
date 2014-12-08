package bankaccount;

import java.io.IOException;
import java.util.ArrayList;

public class Main {
	public static void main(String[] args){
		Replica r = new Replica("54.148.157.54", 8001, 0);
		CLI cli = new CLI(r);
		r.setListener(cli);
		
		try {
			cli.readInput();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		Replica r = new Replica("54.69.100.172", 8001, 1);
//		GCLI cli = new GCLI(r);
//		r.setListener(cli);
//		
//		Replica r = new Replica("54.149.12.69", 8001, 2);
//		GCLI cli = new GCLI(r);
//		r.setListener(cli);
//		
//		Replica r = new Replica("54.69.199.175", 8001, 3);
//		GCLI cli = new GCLI(r);
//		r.setListener(cli);
//		
//		Replica r = new Replica("54.148.74.72", 8001, 4);
//		GCLI cli = new GCLI(r);
//		r.setListener(cli);
	}
}
