package bankaccount;

import java.util.ArrayList;

public class Main {
	public static int appIteration = 0;
	
	public static void main(String[] args){
		ArrayList<Replica> replicas = new ArrayList<Replica>();
		for (int i=0; i<5; i++){
			Replica r = new Replica("localhost", 8001+i, i);
			GCLI cli = new GCLI(r);
			r.setListener(cli);
		}
		
		
//		for (int i=0; i<2; i++){
//			Replica r = new Replica("192.168.0.15", 8001+i, i);
//			r.setReplicaList(replicas);
//			replicas.add(r);
//			CLI2 cli = new CLI2(r);
//			r.setListener(cli);
//		}
//		for (int j=2; j<5; j++){
//			Replica r = new Replica("192.168.0.3", 8001+j, j);
//			r.setReplicaList(replicas);
//			replicas.add(r);
//		}
//		replicas.get(0).becomeLeader();
	}
}
