package com.lanmessager.net.message;

public class SendFileMessage extends Message {
	private static final String KEY_FILE_SIZE = "fileSize";
	private static final String KEY_FILE_NAME = "fileName";
	private static final String KEY_FILE_ID = "fileId";
	private static final String KEY_SENDER_ADDRESS = "senderAddress";
	
	@MessageKey(KEY_FILE_SIZE)
	private long fileSize;

	@MessageKey(KEY_FILE_NAME)
	private String fileName;
	
	@MessageKey(KEY_FILE_ID)
	private String fileId;
	
	@MessageKey(KEY_SENDER_ADDRESS)
	private String senderAddress;

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public String getSenderAddress() {
		return senderAddress;
	}

	public void setSenderAddress(String senderAddress) {
		this.senderAddress = senderAddress;
	}
	
}
