package bankaccount;

import java.util.ArrayList;

public class Main {
	public static void main(String[] args){
		Replica r = new Replica("54.148.157.54", 8001, 0);
		GCLI cli = new GCLI(r);
		r.setListener(cli);
	}
}
