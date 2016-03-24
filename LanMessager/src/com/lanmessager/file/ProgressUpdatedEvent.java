package com.lanmessager.file;

import java.util.EventObject;

public class ProgressUpdatedEvent extends EventObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private long processed;
	
	public ProgressUpdatedEvent(Object source) {
		super(source);
	}

	public long getProcessed() {
		return processed;
	}

	public void setProcessed(long processed) {
		this.processed = processed;
	}	
}
