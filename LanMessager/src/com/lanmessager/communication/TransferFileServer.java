package com.lanmessager.communication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
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
import com.lanmessager.file.FileIdentifier;

public class TransferFileServer {
	private static final Logger LOGGER = Logger.getLogger(TransferFileServer.class.getSimpleName());

	public static final int PORT_TRANSFER_FILE = 12002;

	private static final int BUFFER_LENGTH = 0x1000;
	
	private static final int THREAD_NUMBER = 2;

	private final ExecutorService executor;

	private final Map<String, ReceiveFilePendingTask> pendingTaskMap;
	
	private final Map<String, Future<FileDigestResult>> resultTempMap;
	
	private final Map<String, Future<FileDigestResult>> resultMap;

	private final Map<String, FileProgress> progressMap;

	private ServerSocket serverSocket;

	private boolean isRunning = false;

	public TransferFileServer() {
		executor = Executors.newFixedThreadPool(THREAD_NUMBER);
		pendingTaskMap = new HashMap<>();
		resultTempMap = new HashMap<>();
		resultMap = new HashMap<>();
		progressMap = new HashMap<>();
	}

	public void start() throws IOException {
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

	public void stop() {
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

	/**
	 * Get ready for receiving a file asynchronously.
	 * If remote host send file before local host invoke this method,
	 * the file will be dropped.
	 * 
	 */
	public void receive(String fileId, File file, long fileSize) {
		ReceiveFilePendingTask task = new ReceiveFilePendingTask();
		task.setFile(file);
		task.setFileSize(fileSize);
		
		synchronized (pendingTaskMap) {
			if (pendingTaskMap.containsKey(fileId)) {
				LOGGER.warn("Task exists: " + fileId);
			}
			// Existed task will be dropped.
			pendingTaskMap.put(fileId, task);
		}
	}

	// FIXME Cancel pending task.
	public void cancel(String fileId) {
		synchronized (pendingTaskMap) {
			if (pendingTaskMap.containsKey(fileId)) {
				ReceiveFilePendingTask task = pendingTaskMap.get(fileId);
				task.setCanceled(true);
				return;
			}
		}
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
			File file;
			long fileSize;
			synchronized (resultTempMap) {
				result = resultTempMap.get(tempId);
				resultTempMap.remove(tempId);
				
				synchronized (resultMap) {
					if (resultMap.containsKey(fileId)) {
						LOGGER.warn("Duplicated task: " + fileId);
						return null;
					} else {
						/* Retrieve file information. */
						synchronized (pendingTaskMap) {
							if (!pendingTaskMap.containsKey(fileId)) {
								LOGGER.warn("Pending task doesn't exist: " + fileId);
								return null;
							}
							
							ReceiveFilePendingTask pendingTask = pendingTaskMap.get(fileId);
							file = pendingTask.getFile();
							fileSize = pendingTask.getFileSize();
							/* 
							 * Remove pending task and add result at synchronous block,
							 * to ensure there is always only one of this receiving task information
							 * can be retrieved.
							 */
							pendingTaskMap.remove(fileId);							
							resultMap.put(fileId, result);
						}
					}

				}
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
		private boolean canceled;
		
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
		public boolean isCanceled() {
			return canceled;
		}
		public void setCanceled(boolean canceled) {
			this.canceled = canceled;
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
