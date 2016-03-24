package com.lanmessager.backgroundworker;

import java.util.EventObject;

public class FileProgressUpdatedEvent extends EventObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private long processed;
	private long total;
	private String fileId;
	
	public FileProgressUpdatedEvent(Object source) {
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

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}
}
