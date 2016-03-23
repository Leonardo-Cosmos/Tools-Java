package com.hashfile.ui;

import java.util.EventObject;

public class BackgroundReportEvent extends EventObject {
	
	/**
	 * Serial version UID of this class.
	 */
	private static final long serialVersionUID = 416830681297751368L;
	
	private Object userState;
	
	public BackgroundReportEvent(Object source) {
		this(source, null);
	}

	public BackgroundReportEvent(Object source, Object userState) {
		super(source);
		
		if (!(source instanceof BackgroundWorker)) {
			throw new IllegalArgumentException();
		}
		
		this.userState = userState;
	}
	
	public Object getUserState() {
		return userState;
	}

	public void setUserState(Object userState) {
		this.userState = userState;
	}
}
