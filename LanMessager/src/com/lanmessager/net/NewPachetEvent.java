package com.lanmessager.net;

import java.util.EventObject;

import com.lanmessager.net.packet.Packet;

public class NewPachetEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Packet packet;
	
	public NewPachetEvent(Object source) {
		super(source);
	}

	public Packet getPacket() {
		return packet;
	}

	public void setPacket(Packet packet) {
		this.packet = packet;
	}
	
}
