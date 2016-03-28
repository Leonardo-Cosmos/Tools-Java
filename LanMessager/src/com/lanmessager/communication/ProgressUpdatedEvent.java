package com.lanmessager.communication;

import java.util.EventObject;
// TODO Remove class
public class ProgressUpdatedEvent extends EventObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private long processed;
	
	private long total;
	
	public ProgressUpdatedEvent(Object source) {
		super(source);
	}

	public long getProcessed() {
		return processed;
	}

	public void setProcessed(long processed) {
		this.processed = processed;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}
	
}
