package bankaccount;

public class HeartbeatListener extends Thread {
	private boolean isAlive;
	private long lastHeartbeat;
	private static final int heartbeatTimeout = 5000;;
	private NodeLocationData heartbeatLocationData;
	private Replica replica;
	
	public HeartbeatListener(Replica replica, NodeLocationData locationData) {
		this.replica = replica;
		this.isAlive = true;
		this.lastHeartbeat = System.currentTimeMillis();
		this.heartbeatLocationData = locationData;
	}
	
	public void resetTimeout() {
		lastHeartbeat = System.currentTimeMillis();
	}

	public void run() {
		while(isAlive) {
			if(heartbeatTimeout < System.currentTimeMillis() - lastHeartbeat){
				// if was leader, elect a new one
				if(heartbeatLocationData.isLeader())
					replica.electNewLeader();

				lastHeartbeat = System.currentTimeMillis();
			}
			yield(); // so the while loop doesn't spin too much
		}
	}
	
	public void kill(){
		isAlive = false;
	}
}
