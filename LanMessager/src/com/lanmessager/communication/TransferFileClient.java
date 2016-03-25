package com.lanmessager.communication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;

import com.lanmessager.file.FileDigest;
import com.lanmessager.file.FileDigestResult;

public class TransferFileClient {
	private Logger LOGGER = Logger.getLogger(TransferFileClient.class.getSimpleName());

	public static final byte ID_END = '\0';
	
	private static final int BUFFER_LENGTH = 0x1000;

	private final ExecutorService executor;

	private final Map<String, Future<FileDigestResult>> resultMap;
	
	private final Map<String, Long> progressMap;

	private Set<ProgressUpdatedListener> progressUpdatedListeners;

	public void addProgressUpdatedListener(ProgressUpdatedListener listener) {
		if (progressUpdatedListeners == null) {
			progressUpdatedListeners = new HashSet<>();
		}
		progressUpdatedListeners.add(listener);
	}

	public void removeProgressUpdatedListener(ProgressUpdatedListener listener) {
		if (progressUpdatedListeners != null) {
			progressUpdatedListeners.remove(listener);
		}
	}

	public TransferFileClient() {
		executor = Executors.newCachedThreadPool();
		resultMap = new HashMap<>();
		progressMap = new HashMap<>();
	}

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
		if (!resultMap.containsKey(fileId)) {
			LOGGER.warn("Task doesn't exist: " + fileId);
			return;
		}
		Future<FileDigestResult> result = resultMap.get(fileId);
		result.cancel(true);
	}

	protected void onProgressUpdated(String fileId, long processed, long total) {
		/*if (progressUpdatedListeners != null) {
			ProgressUpdatedEvent event = new ProgressUpdatedEvent(this);
			event.setProcessed(processed);
			event.setTotal(total);
			for (ProgressUpdatedListener listener : progressUpdatedListeners) {
				listener.updateProgress(event);
			}
		}*/
		synchronized (progressMap) {
			progressMap.put(fileId, processed);	
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
