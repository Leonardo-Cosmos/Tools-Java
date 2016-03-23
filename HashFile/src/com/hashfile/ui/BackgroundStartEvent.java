package com.hashfile.ui;

import java.util.EventObject;

public class BackgroundStartEvent extends EventObject {
	
	/**
	 * Serial version UID of this class.
	 */
	private static final long serialVersionUID = -5610264761059348878L;

	private Object parameter;
	
	private Object result;
	
	public BackgroundStartEvent(Object source) {
		this(source, null);
	}
	
	public BackgroundStartEvent(Object source, Object parameter) {
		super(source);
		
		if (!(source instanceof BackgroundWorker)) {
			throw new IllegalArgumentException();
		}
		
		this.parameter = parameter;
	}
	
	public void reportProgress(Object userStage) {
		((BackgroundWorker)super.getSource()).reportProgress(userStage);		
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public Object getParameter() {
		return parameter;
	}
}
