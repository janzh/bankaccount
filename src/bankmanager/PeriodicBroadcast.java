package bankmanager;

import bankaccount.messages.DecideMessage;

// Periodically sends (DecideMessage) to all replicas each
public class PeriodicBroadcast extends Thread{
	private DecideMessage decideMsg;
	private static int decideMsgPeriod = 5000; // Ms of each period between broadcasting decideMsg to all
	private Replica replica;
	
	public PeriodicBroadcast(Replica replica, DecideMessage decideMsg) {
		this.replica = replica;
		this.decideMsg = decideMsg; 
	}
	@Override
    public void run()
    {
        while(true) {
           replica.broadcast(decideMsg);
           try {
			Thread.sleep(decideMsgPeriod);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        }
    }
}