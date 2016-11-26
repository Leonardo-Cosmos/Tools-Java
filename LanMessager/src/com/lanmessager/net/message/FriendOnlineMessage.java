package com.lanmessager.net.message;

public class FriendOnlineMessage extends Message {
	private static final String KEY_NAME = "name";
	private static final String KEY_ADDRESS = "address";
	
	@MessageKey(KEY_NAME)
	private String name;
	
	@MessageKey(KEY_ADDRESS)
	private String address;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
}
