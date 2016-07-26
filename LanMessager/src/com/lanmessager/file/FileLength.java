package com.lanmessager.file;

public class FileLength {
	
	private long length;
	
	private String[] UNITS = {"B", "KB", "MB", "GB"};
	
	/**
	 * Initialize FileLength by file length number.
	 * @param length file length represented by byte.
	 */
	public FileLength(long length) {
		this.length = length;
	}
	
	private String convertUnit(long length) {
		long number = length;
		String unit = null;
		for (int i = 0; i < UNITS.length; i++) {
			unit = UNITS[i];
			
			if (number >> 10 > 0) {
				number >>= 10;
			} else {
				break;
			}
		}
		
		return Long.toString(number).concat(unit);
	}
	
	@Override
	public String toString() {
		return convertUnit(length);
	}
	
}
