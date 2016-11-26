package com.lanmessager.worker;

import java.util.EventObject;

import com.lanmessager.net.message.Message;

public class NewMessageEvent<T extends Message> extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private T message;
	
	public NewMessageEvent(Object source) {
		super(source);
	}

	public T getMessage() {
		return message;
	}

	public void setMessage(T message) {
		this.message = message;
	}

}
