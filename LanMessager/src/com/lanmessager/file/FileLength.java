package com.lanmessager.file;

public class FileLength {
	
	private long length;
	
	private static final String[] UNITS = {"B", "KB", "MB", "GB"};
	
	private static final String FORMAT = "%.3f%s";
	
	/**
	 * Initialize FileLength by file length number.
	 * @param length file length represented by byte.
	 */
	public FileLength(long length) {
		this.length = length;
	}
	
	private String convertUnit(long length) {
		float number = length;
		String unit = UNITS[0];
		for (int i = 1; i < UNITS.length; i++) {			
			if (number / 0x400 > 1.000f) {
				number /= 0x400;
				unit = UNITS[i];
			} else {
				break;
			}
		}
		
		return String.format(FORMAT, number, unit);
	}
	
	@Override
	public String toString() {
		return convertUnit(length);
	}
	
}
