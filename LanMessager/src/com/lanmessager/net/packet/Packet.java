package com.lanmessager.net.packet;

import com.lanmessager.net.message.Message;

public class Packet {
	
	private int type;
	private Message message;

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}
}
