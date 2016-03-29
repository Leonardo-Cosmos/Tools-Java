package com.lanmessager.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.lanmessager.backgroundworker.FileProgress;

public class FileDigestCalculator {
	private Logger LOGGER = Logger.getLogger(FileDigestResult.class.getSimpleName());

	private static final int BUFFER_LENGTH = 0x1000;

	private static final int THREAD_NUMBER = 3;
	
	private final ExecutorService executor;

	private final Map<String, Future<FileDigestResult>> resultMap;

	private final Map<String, FileProgress> progressMap;
	
	private final Lock progressLock;

	public FileDigestCalculator() {
		executor = Executors.newFixedThreadPool(THREAD_NUMBER);
		resultMap = new HashMap<>();
		progressLock = new ReentrantLock();
		progressMap = new HashMap<>();
	}
	
	/**
	 * Start digesting file asynchronously. 
	 *
	 */
	public void digestFile(String fileId, File file) {
		Future<FileDigestResult> result = executor.submit(new DigestFileTask(fileId, file));
		synchronized (resultMap) {
			if (resultMap.containsKey(fileId)) {
				LOGGER.warn("Duplicated task: " + fileId);
			}
			resultMap.put(fileId, result);
		}
	}
	
	public void cancel(String fileId) {
		Future<FileDigestResult> result = null;
		synchronized (resultMap) {
			if (!resultMap.containsKey(fileId)) {
				LOGGER.warn("Task doesn't exist: " + fileId);
				return;
			}
			result = resultMap.get(fileId);
		}
		if (result != null) {
			boolean hasCancelled = result.cancel(true);
			if (!hasCancelled) {
				LOGGER.warn("Task cannot be cancelled: " + fileId);
			}
		}
	}
	
	public boolean isIdle() {
		boolean isIdle = true;
		synchronized (resultMap) {
			if (resultMap.size() > 0) {
				isIdle = false;
			}
		}		
		return isIdle;
	}
	
	/**
	 * Get a map of results which have been done.
	 * <p/>
	 * After reported once, the result cannot be reported again 
	 * and corresponding progress will not be reported.
	 * 
	 */
	public Map<String, Future<FileDigestResult>> reportResult() {
		Map<String, Future<FileDigestResult>> reportMap = new HashMap<>();
		synchronized (resultMap) {
			LOGGER.debug("Result map size: " + resultMap.size());
			resultMap.forEach((id, result) -> {
				if (result.isDone()) {
					reportMap.put(id, result);
				}
			});
			reportMap.forEach((id, result) -> resultMap.remove(id));
		}
		LOGGER.debug("Done result map size: " + reportMap.size());
		if (!reportMap.isEmpty()) {
			progressLock.lock();
			reportMap.forEach((id, result) -> progressMap.remove(id));
			progressLock.unlock();
		}
		return reportMap;
	}
	
	/** Get a map of task progress.
	 * 
	 */
	public Map<String, FileProgress> reportProgress() {
		Map<String, FileProgress> reportMap = new HashMap<>();
		
		progressLock.lock();
		LOGGER.debug("Progress map size: " + progressMap.size());
		progressMap.forEach((id, progress) -> reportMap.put(id, progress));
		progressLock.unlock();
		
		return reportMap;
	}

	protected void onProgressUpdated(String fileId, long processed, long total) {
		FileProgress progress = new FileProgress();
		progress.setProcessed(processed);
		progress.setTotal(total);

		if (progressLock.tryLock()) {
			progressMap.put(fileId, progress);
			progressLock.unlock();
		}
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
			while ((readLength = input.read(buffer, 0, BUFFER_LENGTH)) > 0) {
				fileDigest.update(buffer, 0, readLength);

				updatedLength += readLength;
				
				LOGGER.debug("Update progress: " + fileId);				
				onProgressUpdated(fileId, updatedLength, fileLength);
			}

			result = fileDigest.digest();
		} finally {
			if (input != null) {
				input.close();
			}
		}
		return result;
	}
	
	private class DigestFileTask implements Callable<FileDigestResult> {
		private final String fileId;
		private final File file;
		
		public DigestFileTask(String fileId, File file) {
			this.fileId = fileId;
			this.file = file;
		}
		
		@Override
		public FileDigestResult call() throws Exception {
			return calculate(fileId, file);
		}
	}
}
