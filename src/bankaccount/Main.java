package bankaccount;

public class Main {
	public static int appIteration = 0;
	
	public static void main(String[] args){
		//CLI appInterface = new CLI(0);
		String[] port1 = {"8001"}; 
		CLI2.main(port1);
		String[] port2 = {"8002"};
		CLI2.main(port2);
	}
}
