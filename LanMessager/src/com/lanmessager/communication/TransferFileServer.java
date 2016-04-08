package com.lanmessager.communication;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.lanmessager.backgroundworker.FileProgress;
import com.lanmessager.concurrent.Task;
import com.lanmessager.concurrent.TaskExecutor;
import com.lanmessager.file.FileDigest;
import com.lanmessager.file.FileDigestCalculator;
import com.lanmessager.file.FileDigestResult;
import com.lanmessager.file.FileIdentifier;

import sun.awt.AppContext;

public class TransferFileServer extends TaskExecutor<String, FileDigestResult, FileProgress> {
	private static final Logger LOGGER = Logger.getLogger(TransferFileServer.class.getSimpleName());

	private static final String CLASS_NAME = TransferFileServer.class.getSimpleName();
	
	public static final int PORT_TRANSFER_FILE = 12002;

	private static final int BUFFER_LENGTH = 0x1000;
	
	private static final int THREAD_NUMBER = 2;

	/**
	 * Store receiving file information before there corresponding stock is found.
	 */
	private final Map<String, ReceiveFilePendingTask> pendingTaskMap;

	private ServerSocket serverSocket;

	private boolean isRunning = false;

	private static ExecutorService getWorkersExecutorService() {
		final AppContext appContext = AppContext.getAppContext();
		ExecutorService executorService = (ExecutorService) appContext.get(FileDigestCalculator.class);
		if (executorService == null) {
			ThreadFactory threadFactory = new ThreadFactory() {
				final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
				@Override
				public Thread newThread(Runnable r) {
					Thread thread = defaultFactory.newThread(r);
					thread.setName(CLASS_NAME + "-" + thread.getName());
					thread.setDaemon(true);
					return thread;
				}
			};
			executorService = new ThreadPoolExecutor(THREAD_NUMBER, THREAD_NUMBER, 1L, TimeUnit.MINUTES,
					new LinkedBlockingQueue<>(), threadFactory);
			appContext.put(FileDigestCalculator.class, executorService);
		}
		
		final ExecutorService es = executorService;
		appContext.addPropertyChangeListener(AppContext.DISPOSED_PROPERTY_NAME,
				new PropertyChangeListener() {
					
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						boolean disposed = (Boolean) evt.getNewValue();
						if (disposed) {
							final WeakReference<ExecutorService> executorServiceRef = new WeakReference<ExecutorService>(es);
							final ExecutorService executorService = executorServiceRef.get();
							if (executorService != null) {
								AccessController.doPrivileged(new PrivilegedAction<Void>() {
									public Void run() {
										executorService.shutdown();
										return null;
									};
								});
							}
						}
					}
				});

		return executorService;
	}
	
	public TransferFileServer() {
		super(getWorkersExecutorService());
		pendingTaskMap = new HashMap<>();
	}

	public void start() throws IOException {
		isRunning = true;

		try {
			serverSocket = new ServerSocket(PORT_TRANSFER_FILE);
			LOGGER.info("Transfer file server is listening " + PORT_TRANSFER_FILE + ".");

			Socket client = serverSocket.accept();			
			String tempId = FileIdentifier.generateTemporaryIdentifierString();
			
			ReceiveFileTask task = new ReceiveFileTask(tempId, client);
			// Without real ID, save thread result by temporary ID.
			submit(tempId, task);
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

	@Override
	public void cancel(String fileId) {
		synchronized (pendingTaskMap) {
			if (pendingTaskMap.containsKey(fileId)) {
				ReceiveFilePendingTask task = pendingTaskMap.get(fileId);
				task.setCanceled(true);
				return;
			} else {
				super.cancel(fileId);
			}
		}
	}

	private class ReceiveFileTask extends Task<FileDigestResult, FileProgress> {
		private final String tempId;
		private final Socket socket;

		public ReceiveFileTask(String tempId, Socket socket) {
			this.tempId = tempId;
			this.socket = socket;
		}

		@Override
		public FileDigestResult execute() throws Exception {
			return transfer(tempId, socket);
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
				
				File file;
				long fileSize;
				synchronized (pendingTaskMap) {
					if (!pendingTaskMap.containsKey(fileId)) {
						LOGGER.warn("Pending task doesn't exist: " + fileId);
						return null;
					}
					
					ReceiveFilePendingTask pendingTask = pendingTaskMap.get(fileId);
					file = pendingTask.getFile();
					fileSize = pendingTask.getFileSize();
					if (pendingTask.isCanceled()) {
						/* Cancel self when the task has been cancelled before it is found. */
						cancel();
					}
					/* 
					 * Remove pending task and add result at synchronous block,
					 * to ensure there is always only one of this receiving task information
					 * can be retrieved.
					 */
					pendingTaskMap.remove(fileId);
					
					/* Change task's to real ID. */
					changeKey(tempId, fileId);
				}

				/* Receive real file. */
				FileDigest fileDigest = FileDigest.getInstance();
				output = new FileOutputStream(file);
				while ((readLength = input.read(buffer, 0, BUFFER_LENGTH)) > 0) {
					output.write(buffer, 0, readLength);
					fileDigest.update(buffer, 0, readLength);

					transferedLength += readLength;
					LOGGER.debug("Transfer " + transferedLength + " bytes (total " + fileSize + " bytes).");

					FileProgress progress = new FileProgress();
					progress.setProcessed(transferedLength);
					progress.setTotal(fileSize);
					onStatusUpdated(progress);
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
