package com.lanmessager.net;

public class FileUncompletedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private long processed;
	
	private long total;
	
	public FileUncompletedException() {
		
	}
	
	public FileUncompletedException(long processed, long total) {
		this(processed, total, null);
	}
	
	public FileUncompletedException(String message) {
		this(0, 0, message);
	}
	
	public FileUncompletedException(long processed, long total, String message) {
		super(message);
		
		this.processed = processed;
		this.total = total;
	}

	public long getProcessed() {
		return processed;
	}

	public long getTotal() {
		return total;
	}
}
