package com.hashfile.ui;

import java.util.EventObject;

public class BackgroundCompleteEvent extends EventObject {
	
	/**
	 * Serial version UID of this class.
	 */
	private static final long serialVersionUID = -6766558349042993424L;

	private Throwable error;
	
	private Object result;
	
	public BackgroundCompleteEvent(Object source) {
		this(source, null, null);
	}
	
	public BackgroundCompleteEvent(Object source, Object result, Throwable error) {
		super(source);
		
		if (!(source instanceof BackgroundWorker)) {
			throw new IllegalArgumentException();
		}
		
		this.result = result;
		this.error = error;
	}

	public Throwable getError() {
		return error;
	}

	public Object getResult() {
		return result;
	}
}
