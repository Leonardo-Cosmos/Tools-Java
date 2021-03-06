package com.hashfile.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

/**
 * A class that can be used to compute the MD5, SHA1, SHA-512, CRC-32 of a data stream.
 * @author Leonardo
 *
 */
public class Hash {

	private MessageDigest md5;
	
	private byte[] md5Value;
	
	private MessageDigest sha1;
	
	private byte[] sha1Value;
	
	private MessageDigest sha256;
	
	private byte[] sha256Value;
	
	private MessageDigest sha512;
	
	private byte[] sha512Value;
	
	private CRC32 crc32;
	
	private long crc32Value;
	
	/**
	 * Constructs a new Hash object.
	 */
	public Hash() {
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		try {
			sha1 = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		try {
			sha512 = MessageDigest.getInstance("SHA-512");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		crc32 = new CRC32();
		
		md5Value = null;
		sha1Value = null;
		sha256Value = null;
		sha512Value = null;
		crc32Value = 0;
	}
	
	/**
	 * Resets the hash value.
	 */
	public void reset() {
		md5.reset();
		sha1.reset();
		sha256.reset();
		sha512.reset();
		crc32.reset();
	}
	
	/**
	 * Updates the hash value with the specified array of bytes.
	 * @param input - the array of bytes to update the hash value with
	 */
	public void update(byte[] input) {
		md5.update(input);
		if (md5Value != null) {
			md5Value = null;
		}
		
		sha1.update(input);
		if (sha1Value != null) {
			sha1Value = null;
		}
		
		sha256.update(input);
		if (sha256Value != null) {
			sha256Value = null;
		}
		
		sha512.update(input);
		if (sha512Value != null) {
			sha512Value = null;
		}
		
		crc32.update(input);
		if (crc32Value != 0) {
			crc32Value = 0;
		}
	}
	
	/**
	 * Updates the hash value with the specified array of bytes.
	 * @param input - the byte array to update the hash value with
	 * @param offset - the start offset of the array of bytes
	 * @param length - the number of bytes to use for the update
	 */
	public void update(byte[] input, int offset, int length) {
		md5.update(input, offset, length);
		if (md5Value != null) {
			md5Value = null;
		}
		
		sha1.update(input, offset, length);
		if (sha1Value != null) {
			sha1Value = null;
		}
		
		sha256.update(input, offset, length);
		if (sha256Value != null) {
			sha256Value = null;
		}
		
		sha512.update(input, offset, length);
		if (sha512Value != null) {
			sha512Value = null;
		}
		
		crc32.update(input, offset, length);
		if (crc32Value != 0) {
			crc32Value = 0;
		}
	}
	
	/**
	 * Computes and returns MD5 value.
	 * @return a byte array of MD5 value.
	 */
	public byte[] getMD5Value() {
		if (md5Value == null) {
			md5Value = md5.digest();
		}
		return md5Value;
	}
	
	/**
	 * Returns a hexadecimal string representation of MD5 value. 
	 * @return MD5 value.
	 */
	public String getMD5Text() {
		return getHexText(getMD5Value());
	}
	
	/**
	 * Computes and returns SHA1 value.
	 * @return a byte array of SHA1 value.
	 */
	public byte[] getSHA1Value() {
		if (sha1Value == null) {
			sha1Value = sha1.digest(); 
		}
		return sha1Value;
	}
	
	/** 
	 * Returns a hexadecimal string representation of SHA1 value.
	 * @return SHA1 value.
	 */
	public String getSHA1Text() {
		return getHexText(getSHA1Value());
	}
	
	/**
	 * Computes and returns SHA-256 value.
	 * @return a byte array of SHA-256 value.
	 */
	public byte[] getSHA256Value() {
		if (sha256Value == null) {
			sha256Value = sha256.digest(); 
		}
		return sha256Value;
	}
	
	/** 
	 * Returns a hexadecimal string representation of SHA-256 value.
	 * @return SHA-256 value.
	 */
	public String getSHA256Text() {
		return getHexText(getSHA256Value());
	}
	
	/**
	 * Computes and returns SHA-512 value.
	 * @return a byte array of SHA-512 value.
	 */
	public byte[] getSHA512Value() {
		if (sha512Value == null) {
			sha512Value = sha512.digest(); 
		}
		return sha512Value;
	}
	
	/** 
	 * Returns a hexadecimal string representation of SHA-512 value.
	 * @return SHA-512 value.
	 */
	public String getSHA512Text() {
		return getHexText(getSHA512Value());
	}
	
	/**
	 * Returns CRC-32 value.
	 * @return CRC-32 value.
	 */
	public long getCRC32Value() {
		if (crc32Value == 0) {
			crc32Value = crc32.getValue();			
		}
		return crc32Value;
	}
	
	/**
	 * Returns a hexadecimal string representation of CRC-32 value.
	 * @return CRC-32 value.
	 */
	public String getCRC32Text() {
		return String.format("%1$08x", getCRC32Value());
	}
	
	private String getHexText(byte[] bytes) {
		StringBuffer strBuffer = new StringBuffer();
		for (byte aByte : bytes) {
			strBuffer.append(String.format("%1$02x", aByte));
		}
		return strBuffer.toString();
	}
}
