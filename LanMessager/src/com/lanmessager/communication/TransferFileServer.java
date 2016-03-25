package com.lanmessager.communication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
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
import com.lanmessager.file.FileIdentifier;

public class TransferFileServer {
	private static final Logger LOGGER = Logger.getLogger(TransferFileServer.class.getSimpleName());

	public static final int PORT_TRANSFER_FILE = 12002;

	private static final int BUFFER_LENGTH = 0x1000;

	private final ExecutorService executor;

	private final Map<String, ReceiveFilePendingTask> taskMap;
	
	private final Map<String, Future<FileDigestResult>> resultTempMap;
	
	private final Map<String, Future<FileDigestResult>> resultMap;

	private final Map<String, Long> progressMap;

	private ServerSocket serverSocket;

	private boolean isRunning = false;

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

	public TransferFileServer() {
		executor = Executors.newCachedThreadPool();
		taskMap = new HashMap<>();
		resultTempMap = new HashMap<>();
		resultMap = new HashMap<>();
		progressMap = new HashMap<>();
	}

	public void start() throws IOException, FileUncompletedException {
		isRunning = true;

		try {
			serverSocket = new ServerSocket(PORT_TRANSFER_FILE);
			LOGGER.info("Transfer file server is listening " + PORT_TRANSFER_FILE + ".");

			Socket client = serverSocket.accept();			
			String tempId = FileIdentifier.generateTemporaryIdentifierString();
			
			Future<FileDigestResult> result = executor.submit(new ReceiveFileTask(tempId, client));
			// Without real ID, save thread result by temporary ID.
			synchronized (resultTempMap) {
				resultTempMap.put(tempId, result);
			}
		} finally {
			if (isRunning) {
				stop();
			}
		}

	}

	private void stop() {
		if (isRunning) {
			isRunning = false;
			try {
				if (serverSocket != null) {
					serverSocket.close();
					serverSocket = null;
				}
			} catch (IOException ex) {
				LOGGER.error("Failed to close socket.", ex);
			}
		}
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void receive(String fileId, File file, long fileSize) {
		ReceiveFilePendingTask task = new ReceiveFilePendingTask();
		task.setFile(file);
		task.setFileSize(fileSize);
		
		synchronized (taskMap) {
			if (taskMap.containsKey(fileId)) {
				LOGGER.warn("Task exists: " + fileId);
			}
			// Existed task will be dropped.
			taskMap.put(fileId, task);
		}
	}

	private FileDigestResult transfer(String tempId, Socket socket)
			throws IOException, FileUncompletedException {
		InputStream input = null;
		OutputStream output = null;
		FileDigestResult fileDigestResult = null;
		try {
			input = socket.getInputStream();
			byte[] buffer = new byte[BUFFER_LENGTH];
			int readLength = 0;
			long transferedLength = 0;
			
			/* Read ID from head of input. */
			readLength = input.read(buffer, 0, BUFFER_LENGTH);
			String fileId = null;
			for (int i = 0; i < readLength; i++) {
				if (TransferFileClient.ID_END == buffer[i]) {
					fileId = new String(buffer, 0, i - 1);
					break;
				}
			}
			if (fileId == null) {
				throw new FileUncompletedException("Cannot read file ID.");
			}
			
			/* Move thread result to the map whose key is real ID. */
			Future<FileDigestResult> result = null;
			synchronized (resultTempMap) {
				result = resultTempMap.get(tempId);
			}
			
			synchronized (resultMap) {
				if (resultMap.containsKey(fileId)) {
					LOGGER.warn("Duplicated task: " + fileId);
				}
				resultMap.put(fileId, result);
			}
			
			/* Retrieve file information. */
			File file;
			long fileSize;
			synchronized (taskMap) {
				if (!taskMap.containsKey(fileId)) {
					LOGGER.warn("Pending task doesn't exist: " + fileId);
					return null;
				}
				ReceiveFilePendingTask pendingTask = taskMap.get(fileId);
				file = pendingTask.getFile();
				fileSize = pendingTask.getFileSize();
			}

			/* Receive real file. */
			FileDigest fileDigest = FileDigest.getInstance();
			output = new FileOutputStream(file);
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
						"Receiving file is uncompleted.");
				throw ex;
			}
		} finally {
			if (output != null) {
				try {

					output.close();
				} catch (IOException ex) {
					LOGGER.error("Failed to close file output stream.", ex);
				}
			}

			if (input != null) {
				try {
					input.close();
				} catch (IOException ex) {
					LOGGER.error("Failed to close socket input stream.", ex);
				}
			}

			if (socket != null) {
				try {
					socket.close();
				} catch (IOException ex) {
					socket = null;
					LOGGER.error("Failed to close socket from client.", ex);
				}
			}
		}
		return fileDigestResult;
	}

	protected void onProgressUpdated(String fileId, long processed, long total) {
		/*
		 * if (progressUpdatedListeners != null) { for (ProgressUpdatedListener
		 * listener : progressUpdatedListeners) { ProgressUpdatedEvent event =
		 * new ProgressUpdatedEvent(this); event.setProcessed(processed);
		 * event.setTotal(total); listener.updateProgress(event); } }
		 */
		synchronized (progressMap) {
			progressMap.put(fileId, processed);	
		}
	}

	private class ReceiveFileTask implements Callable<FileDigestResult> {
		private final String tempId;
		private final Socket socket;

		public ReceiveFileTask(String tempId, Socket socket) {
			this.tempId = tempId;
			this.socket = socket;
		}

		@Override
		public FileDigestResult call() throws Exception {
			return transfer(tempId, socket);
		}
	}
	
	private class ReceiveFilePendingTask {
		private File file;
		private long fileSize;
		
		public File getFile() {
			return file;
		}
		public void setFile(File file) {
			this.file = file;
		}
		public long getFileSize() {
			return fileSize;
		}
		public void setFileSize(long fileSize) {
			this.fileSize = fileSize;
		}		
	}
	
	/*private class ReceiveFileBlockingTask {
		private Socket socket;

		public Socket getSocket() {
			return socket;
		}

		public void setSocket(Socket socket) {
			this.socket = socket;
		}		
	}*/
}
