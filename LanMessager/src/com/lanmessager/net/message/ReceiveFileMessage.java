package com.lanmessager.net.message;

public class ReceiveFileMessage extends Message {
	private static final String KEY_ACCEPT = "accept";
	private static final String KEY_PORT = "receiverPort";
	private static final String KEY_FILE_ID = "fileId";
	
	@MessageKey(KEY_ACCEPT)
	private boolean accept;
	
	@MessageKey(KEY_PORT)
	private int receiverPort;
	
	@MessageKey(KEY_FILE_ID)
	private String fileId;

	public boolean isAccept() {
		return accept;
	}

	public void setAccept(boolean accept) {
		this.accept = accept;
	}

	public int getReceiverPort() {
		return receiverPort;
	}

	public void setReceiverPort(int port) {
		this.receiverPort = port;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

}
