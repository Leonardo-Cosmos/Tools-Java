package com.lanmessager.net.message;

public class ReceiveFileMessage extends Message {
	private static final String KEY_ACCEPT = "accept";
	private static final String KEY_PORT = "receiverPort";
	private static final String KEY_FILE_ID = "fileId";
	
	private static final String KEY_DIR_ID = "dirId";
	
	@MessageKey(KEY_ACCEPT)
	private boolean accept;
	
	@MessageKey(KEY_PORT)
	private int receiverPort;
	
	@MessageKey(KEY_FILE_ID)
	private String fileId;
	
	@MessageKey(KEY_DIR_ID)
	private String dirId;

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

	public String getDirId() {
		return dirId;
	}

	public void setDirId(String dirId) {
		this.dirId = dirId;
	}

}
