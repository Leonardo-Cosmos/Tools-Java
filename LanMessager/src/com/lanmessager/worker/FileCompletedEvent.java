package com.lanmessager.worker;

import java.util.EventObject;

import com.lanmessager.file.FileDigestResult;

public class FileCompletedEvent extends EventObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private FileDigestResult fileDigestResult;
	private boolean isCancelled = false;
	private boolean isFailed = false;
	private Throwable cause;
	private String fileId;
	
	public FileCompletedEvent(Object source) {
		super(source);
	}

	public FileDigestResult getFileDigestResult() {
		return fileDigestResult;
	}

	public void setFileDigestResult(FileDigestResult fileDigestResult) {
		this.fileDigestResult = fileDigestResult;
	}

	public boolean isCancelled() {
		return isCancelled;
	}

	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}

	public boolean isFailed() {
		return isFailed;
	}

	public void setFailed(boolean isFailed) {
		this.isFailed = isFailed;
	}

	public Throwable getCause() {
		return cause;
	}

	public void setCause(Throwable exception) {
		this.cause = exception;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}
}
