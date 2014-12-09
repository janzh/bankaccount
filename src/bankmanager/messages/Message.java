package bankmanager.messages;

import java.io.Serializable;

import bankmanager.NodeLocationData;

public abstract class Message implements Serializable{
	protected NodeLocationData sender;
	protected NodeLocationData receiver;
	
	public NodeLocationData getSender() {
		return sender;
	}

	public void setSender(NodeLocationData sender) {
		this.sender = sender;
	}

	public NodeLocationData getReceiver() {
		return receiver;
	}
	
	public void setReceiver(NodeLocationData receiver) {
		this.receiver = receiver;
	}
}

