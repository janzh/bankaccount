package bankaccount;

import java.util.ArrayList;

public class Main {
	public static int appIteration = 0;
	
	public static void main(String[] args){
		ArrayList<Replica> replicas = new ArrayList<Replica>();
		for (int i=0; i<3; i++){
			Replica r = new Replica("localhost", 8001+i, i);
			r.setReplicaList(replicas);
			replicas.add(r);
			CLI2 cli = new CLI2(r);
			r.setListener(cli);
		}
	}
}
