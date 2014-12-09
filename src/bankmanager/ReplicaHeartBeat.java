package bankmanager;

import java.util.Random;

import bankmanager.messages.HeartbeatMessage;

public class ReplicaHeartBeat extends Thread {
	
	private long lastHeartbeat;
	private Random rand;
	private Replica replica;
	
	// TODO: Testing variables for fail/unfail commands
	private static final int heartbeatDelayMin = 500;
	private static final int heartbeatDelayMax = 1000;
	
	public ReplicaHeartBeat(Replica replica) {
		this.replica = replica;
		lastHeartbeat = System.currentTimeMillis();
		rand = new Random();
	}

	public void run() {
		int heartbeatDelay = rand.nextInt(heartbeatDelayMax - heartbeatDelayMin) + heartbeatDelayMin;
		while(replica.isAlive()) {
			if(heartbeatDelay < System.currentTimeMillis() - lastHeartbeat) {
				HeartbeatMessage heartbeatMessage = new HeartbeatMessage();
				replica.broadcast(heartbeatMessage);
				lastHeartbeat = System.currentTimeMillis();
				heartbeatDelay = rand.nextInt(heartbeatDelayMax - heartbeatDelayMin) + heartbeatDelayMin;
			}
			try {
				sleep(0);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // so the while loop doesn't spin too much
		}
	}
}