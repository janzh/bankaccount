package bankaccount;

import java.util.Random;

import bankaccount.messages.HeartbeatMessage;

public class ReplicaHeartBeat extends Thread {
	
	private long lastHeartbeat;
	private Random rand;
	private Replica replica;
	
	private static final int heartbeatDelayMin = 1000;
	private static final int heartbeatDelayMax = 2000;
	
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
			yield(); // so the while loop doesn't spin too much
		}
	}
}