package bankaccount;

import java.util.ArrayList;

public class Main {
	public static void main(String[] args){
		Replica r = new Replica("54.148.157.54", 8001, 0);
		GCLI cli = new GCLI(r);
		r.setListener(cli);
		
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
