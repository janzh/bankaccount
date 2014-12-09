package bankaccount;

public class HeartbeatListener extends Thread {
	private boolean isAlive;
	private long lastHeartbeat;
	private static final int heartbeatTimeout = 15000;
	private NodeLocationData heartbeatLocationData;
	private Replica replica;
	
	public HeartbeatListener(Replica replica, NodeLocationData locationData) {
		this.replica = replica;
		this.isAlive = true;
		this.lastHeartbeat = System.currentTimeMillis();
		this.heartbeatLocationData = locationData;
	}
	
	public void resetTimeout() {
		System.out.println("Timeout was reset");
		lastHeartbeat = System.currentTimeMillis();
	}

	public void run() {
		while(isAlive && !interrupted()) {
			if(heartbeatTimeout < System.currentTimeMillis() - lastHeartbeat){
				// if was leader, elect a new one
				if(heartbeatLocationData.isLeader())
					replica.electNewLeader();

				resetTimeout();
			}
			try {
				sleep(0);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // so the while loop doesn't spin too much
		}
	}
	
	public void kill(){
		isAlive = false;
	}
}
