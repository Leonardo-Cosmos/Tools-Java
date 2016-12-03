package com.lanmessager.ui;

public abstract class ProcessPanel extends ChatMessagePanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final String STATUS_INITIALIZE = "Initialize";
	public static final String STATUS_ABORT = "Abort";
	public static final String STATUS_START = "Start";
	public static final String STATUS_SUCCEED = "Succeed";
	public static final String STATUS_FAIL = "Fail";
	public static final String STATUS_CANCEL = "Cancel";
	
	private String status;
	
	public ProcessPanel() {
		status = STATUS_INITIALIZE;
	}
	
	public void succeed(Object result) {
		status = STATUS_SUCCEED;
	}
	
	public void fail(Object cause) {
		status = STATUS_FAIL;
	}
	
	public void abort() {
		status = STATUS_ABORT;
	}
	
	public void start() {
		status = STATUS_START;
	}
	
	public void cancel() {
		status = STATUS_CANCEL;
	}

	public String getStatus() {
		return status;
	}
}
