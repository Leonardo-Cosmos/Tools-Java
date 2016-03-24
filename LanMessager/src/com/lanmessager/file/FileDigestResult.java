package com.lanmessager.file;

public class FileDigestResult {

	private byte[] md5;
	
	private byte[] sha1;
	
	private byte[] sha256;

	public FileDigestResult(byte[] md5, byte[] sha1, byte[] sha256) {
		this.md5 = md5;
		this.sha1 = sha1;
		this.sha256 = sha256;
	}
	
	public byte[] getMd5() {
		return md5;
	}

	void setMd5(byte[] md5) {
		this.md5 = md5;
	}

	public byte[] getSha1() {
		return sha1;
	}

	void setSha1(byte[] sha1) {
		this.sha1 = sha1;
	}
	
	public byte[] getSha256() {
		return sha256;
	}

	public void setSha256(byte[] sha256) {
		this.sha256 = sha256;
	}

	public String getMd5HexString() {
		return bytesToHex(getMd5());
	}
	
	public String getSha1HexString() {
		return bytesToHex(getSha1());
	}
	
	public String getSha256HexString() {
		return bytesToHex(getSha256());
	}
	
	private String bytesToHex(byte[] bytes) {
		StringBuilder hexBuilder = new StringBuilder();
		int digital;
		for (int i = 0; i < bytes.length; i++) {
			digital = bytes[i];
			if (digital < 0) {
				digital += 0x100;
			}
			if (digital < 0x10) {
				hexBuilder.append("0");
			}
			hexBuilder.append(Integer.toHexString(digital));
		}
		return hexBuilder.toString();
	}
}
