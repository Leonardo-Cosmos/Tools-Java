package com.lanmessager.file;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class FileIdentifier {
	//private static final Logger LOGGER = Logger.getLogger(FileIdentifier.class.getSimpleName());
	
	private static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmsss");
	
	private static int RANDOM_POSTFIX_BOUND = 0x100;
	
	private static String RANDOM_POSTFIX_FORMAT = "%08x";
	
	//private static final String DIGEST_ALGORITHM = "MD5";
	
	private FileIdentifier() {
		
	}
	
	public static String generateIdentifierString(File file) {
		StringBuilder idBuilder = new StringBuilder(); 
		idBuilder.append(file.getName());
		idBuilder.append(DATE_FORMAT.format(new Date()));
		
		Random random = new Random();
		String randomPostfix = String.format(RANDOM_POSTFIX_FORMAT, 
				random.nextInt(RANDOM_POSTFIX_BOUND));
		idBuilder.append(randomPostfix);
		
		return idBuilder.toString();
	}
	
	/*public static FileIdentifier generateFileIdentifier(File file) {
		StringBuilder idBuilder = new StringBuilder(); 
		idBuilder.append(file.getName());
		idBuilder.append(DATE_FORMAT.format(new Date()));
		
		Random random = new Random();
		String randomPostfix = String.format(RANDOM_POSTFIX_FORMAT, 
				random.nextInt(RANDOM_POSTFIX_BOUND));
		idBuilder.append(randomPostfix);
		
		FileIdentifier identifier = new FileIdentifier();
		try {
			MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
			identifier.id = digest.digest(idBuilder.toString().getBytes());
		} catch (NoSuchAlgorithmException ex) {
			LOGGER.error("Failed to initialize MD5 digest.", ex);
		}
		return identifier;
	}*/
}
