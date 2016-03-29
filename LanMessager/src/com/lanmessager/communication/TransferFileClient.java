package com.lanmessager.communication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;

import com.lanmessager.backgroundworker.FileProgress;
import com.lanmessager.file.FileDigest;
import com.lanmessager.file.FileDigestResult;

public class TransferFileClient {
	private Logger LOGGER = Logger.getLogger(TransferFileClient.class.getSimpleName());

	public static final byte ID_END = '\0';
	
	private static final int BUFFER_LENGTH = 0x1000;
	
	private static final int THREAD_NUMBER = 2;

	private final ExecutorService executor;

	private final Map<String, Future<FileDigestResult>> resultMap;
	
	private final Map<String, FileProgress> progressMap;

	public TransferFileClient() {
		executor = Executors.newFixedThreadPool(THREAD_NUMBER);
		resultMap = new HashMap<>();
		progressMap = new HashMap<>();
	}

	/**
	 * Start sending a file asynchronously.
	 * 
	 */
	public void send(String fileId, String remoteHost, File file, long fileSize) {
		Future<FileDigestResult> result = executor.submit(new SendFileTask(fileId, remoteHost, file, fileSize));
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
			resultMap.forEach((id, result) -> {
				if (result.isDone()) {
					reportMap.put(id, result);
				}
			});
			reportMap.forEach((id, result) -> resultMap.remove(id));
		}
		if (!reportMap.isEmpty()) {
			synchronized (progressMap) {
				reportMap.forEach((id, result) -> progressMap.remove(id));
			}
		}
		return reportMap;
	}
	
	/** Get a map of task progress.
	 * 
	 */
	public Map<String, FileProgress> reportProgress() {
		Map<String, FileProgress> reportMap = new HashMap<>();
		synchronized (progressMap) {
			progressMap.forEach((id, progress) -> reportMap.put(id, progress));
		}
		return reportMap;
	}

	protected void onProgressUpdated(String fileId, long processed, long total) {
		synchronized (progressMap) {
			FileProgress progress = new FileProgress();
			progress.setProcessed(processed);
			progress.setTotal(total);
			progressMap.put(fileId, progress);	
		}
	}
	
	private FileDigestResult transfer(String fileId, String remoteHost, File file, long fileSize)
			throws IOException, FileUncompletedException {
		Socket socket = null;
		InputStream input = null;
		OutputStream output = null;
		FileDigestResult fileDigestResult = null;
		try {
			socket = new Socket(remoteHost, TransferFileServer.PORT_TRANSFER_FILE);
			output = socket.getOutputStream();
			
			/* Write ID to head of output. */
			output.write(fileId.getBytes());
			output.write(new byte[] {ID_END});

			FileDigest fileDigest = FileDigest.getInstance();
			input = new FileInputStream(file);
			byte[] buffer = new byte[BUFFER_LENGTH];
			int readLength = 0;
			long transferedLength = 0;
			
			/* Send real file. */
			while ((readLength = input.read(buffer, 0, BUFFER_LENGTH)) > 0) {
				output.write(buffer, 0, readLength);
				fileDigest.update(buffer, 0, readLength);

				transferedLength += readLength;
				LOGGER.debug("Transfer " + transferedLength + " bytes (total " + fileSize + " bytes).");

				onProgressUpdated(fileId, transferedLength, fileSize);
			}

			if (transferedLength == fileSize) {
				fileDigestResult = fileDigest.digest();
			} else {
				FileUncompletedException ex = new FileUncompletedException(transferedLength, fileSize,
						"Sending file is uncompleted.");
				throw ex;
			}
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException ex) {
					LOGGER.error("Failed to close file input stream.", ex);
				}
			}

			if (output != null) {
				try {
					output.close();
				} catch (IOException ex) {
					LOGGER.error("Failed to close socket output stream.", ex);
				}
			}

			if (socket != null) {
				try {
					socket.close();
				} catch (IOException ex) {
					socket = null;
					LOGGER.error("Failed to close socket to server.", ex);
				}
			}
		}
		return fileDigestResult;
	}

	private class SendFileTask implements Callable<FileDigestResult> {
		private final String fileId;
		private final String remoteHost;
		private final File file;
		private final long fileSize;

		public SendFileTask(String fileId, String remoteHost, File file, long fileSize) {
			this.fileId = fileId;
			this.remoteHost = remoteHost;
			this.file = file;
			this.fileSize = fileSize;
		}

		@Override
		public FileDigestResult call() throws Exception {
			return transfer(fileId, remoteHost, file, fileSize);
		}		
	}
}
