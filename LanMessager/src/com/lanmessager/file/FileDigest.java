package com.lanmessager.file;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

public class FileDigest {

	private static final Logger LOGGER = Logger.getLogger(FileDigest.class.getSimpleName());

	private static final String MD5 = "MD5";

	private static final String SHA1 = "SHA-1";
	
	private static final String SHA256 = "SHA-256";

	private final MessageDigest md5Digest;

	private final MessageDigest sha1Digest;
	
	private final MessageDigest sha256Digest;

	public static FileDigest getInstance() {
		MessageDigest md5Digest = null;
		try {
			md5Digest = MessageDigest.getInstance(MD5);
		} catch (NoSuchAlgorithmException ex) {
			LOGGER.error("Failed to initialize MD5 digest.", ex);
		}
		
		MessageDigest sha1Digest = null;
		try {
			sha1Digest = MessageDigest.getInstance(SHA1);
		} catch (NoSuchAlgorithmException ex) {
			LOGGER.error("Failed to initialize SHA-1 digest.", ex);
		}
		
		MessageDigest sha256Digest = null;
		try {
			sha256Digest = MessageDigest.getInstance(SHA256);
		} catch (NoSuchAlgorithmException ex) {
			LOGGER.error("Failed to initialize SHA-256 digest.", ex);
		}
		
		FileDigest instance = new FileDigest(md5Digest, sha1Digest, sha256Digest);
		return instance;
	}

	private FileDigest(MessageDigest md5Digest, MessageDigest sha1Digest, MessageDigest sha256Digest) {
		this.md5Digest = md5Digest;
		this.sha1Digest = sha1Digest;
		this.sha256Digest = sha256Digest;
	}

	public void update(byte[] input, int offset, int len) {
		md5Digest.update(input, offset, len);
		sha1Digest.update(input, offset, len);
		sha256Digest.update(input, offset, len);
	}

	public FileDigestResult digest() {
		byte[] md5 = md5Digest.digest();
		byte[] sha1 = sha1Digest.digest();
		byte[] sha256 = sha256Digest.digest();

		return new FileDigestResult(md5, sha1, sha256);
	}

	public FileDigestResult digest(byte[] input) {
		byte[] md5 = md5Digest.digest(input);
		byte[] sha1 = sha1Digest.digest(input);
		byte[] sha256 = sha256Digest.digest(input);
		return new FileDigestResult(md5, sha1, sha256);
	}	
	
}
