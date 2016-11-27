package com.lanmessager.net.message;

public class SendDirMessage extends Message {
	private static final String KEY_DIR_NAME = "dirName";
	private static final String KEY_DIR_ID = "dirId";
	private static final String KEY_SENDER_ADDRESS = "senderAddress";
	
	@MessageKey(KEY_DIR_NAME)
	private String dirName;
	
	@MessageKey(KEY_DIR_ID)
	private String dirId;
	
	@MessageKey(KEY_SENDER_ADDRESS)
	private String senderAddress;

	public String getDirName() {
		return dirName;
	}

	public void setDirName(String dirName) {
		this.dirName = dirName;
	}

	public String getDirId() {
		return dirId;
	}

	public void setDirId(String dirId) {
		this.dirId = dirId;
	}

	public String getSenderAddress() {
		return senderAddress;
	}

	public void setSenderAddress(String senderAddress) {
		this.senderAddress = senderAddress;
	}
	
}
