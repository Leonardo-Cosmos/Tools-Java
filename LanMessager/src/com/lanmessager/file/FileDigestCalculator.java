package com.lanmessager.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.log4j.Logger;

import com.lanmessager.backgroundworker.FileProgress;
import com.lanmessager.concurrent.Task;
import com.lanmessager.concurrent.TaskExecutor;

public class FileDigestCalculator extends TaskExecutor<String, FileDigestResult, FileProgress> {
	private static final Logger LOGGER = Logger.getLogger(FileDigestCalculator.class.getSimpleName());
	
	private static final int BUFFER_LENGTH = 0x1000;

	private static final int THREAD_NUMBER = 3;

	public FileDigestCalculator() {
		super(THREAD_NUMBER);		
	}
	
	/**
	 * Start digesting file asynchronously. 
	 *
	 */
	public void digestFile(String fileId, File file) {
		DigestFileTask task = new DigestFileTask(fileId, file);
		submit(fileId, task);
	}
	
	private class DigestFileTask extends Task<FileDigestResult, FileProgress> {
		private final String fileId;
		private final File file;
		
		public DigestFileTask(String fileId, File file) {
			this.fileId = fileId;
			this.file = file;
		}
		
		@Override
		public FileDigestResult execute() throws Exception {
			return calculate(fileId, file);
		}

		private FileDigestResult calculate(String fileId, File file) throws IOException {
			FileDigest fileDigest = FileDigest.getInstance();
			InputStream input = null;
			FileDigestResult result = null;
			try {
				input = new FileInputStream(file);
				long fileLength = file.length();
				byte[] buffer = new byte[BUFFER_LENGTH];
				int readLength = 0;
				long updatedLength = 0;
				while (!isCancelled() && (readLength = input.read(buffer, 0, BUFFER_LENGTH)) > 0) {
					fileDigest.update(buffer, 0, readLength);

					updatedLength += readLength;
					FileProgress progress = new FileProgress();
					progress.setProcessed(updatedLength);
					progress.setTotal(fileLength);
					onStatusUpdated(progress);
				}

				if (!isCancelled()) {
					result = fileDigest.digest();
				} else {
					LOGGER.debug("Task is cancelled: " + fileId);
					result = null;
				}
			} finally {
				if (input != null) {
					input.close();
				}
			}
			return result;
		}
	}
}
