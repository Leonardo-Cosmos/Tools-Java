package com.lanmessager.module;

public class FriendInfo {

	private static final String TO_STRING_FORMAT = "%s(%s)";
	
	private String name;
	
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
	
	@Override
	public String toString() {
		return String.format(TO_STRING_FORMAT, name, address);
	}
}
