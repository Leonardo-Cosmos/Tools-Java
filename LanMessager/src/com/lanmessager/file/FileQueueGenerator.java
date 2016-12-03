package com.lanmessager.file;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.Queue;

public class FileQueueGenerator {

	private static final FileFilter ONLY_FILE_FILTER = file -> file.isFile();
	private static final FileFilter ONLY_DIR_FILTER = file -> file.isDirectory();
	
	public static Queue<String> getFileQueueOfDir(File dir) {
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("Argument should be a directory.");
		}
		
		Queue<String> fileQueue = new LinkedList<>();
		offerFiles(dir, fileQueue);
		
		return fileQueue;
	}
	
	private static void offerFiles(File baseDir, Queue<String> fileQueue) {
		File[] files = baseDir.listFiles(ONLY_FILE_FILTER);
		for (File file : files) {
			fileQueue.offer(file.getAbsolutePath());
		}
		
		File[] dirs = baseDir.listFiles(ONLY_DIR_FILTER);
		for (File dir : dirs) {
			offerFiles(dir, fileQueue);
		}
	}

}
