package com.lanmessager.communication;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedAction;
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

import sun.awt.AppContext;

public class TransferFileClient extends TaskExecutor<String, FileDigestResult, FileProgress> {
	private Logger LOGGER = Logger.getLogger(TransferFileClient.class.getSimpleName());
	
	public static final byte ID_END = '\0';

	private static final String CLASS_NAME = TransferFileClient.class.getSimpleName();
	
	private static final int BUFFER_LENGTH = 0x1000;
	
	private static final int THREAD_NUMBER = 2;

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
	
	public TransferFileClient() {
		super(getWorkersExecutorService());
	}

	/**
	 * Start sending a file asynchronously.
	 * 
	 */
	public void send(String fileId, String remoteHost, File file, long fileSize) {
		SendFileTask task = new SendFileTask(fileId, remoteHost, file, fileSize);
		submit(fileId, task);
	}

	private class SendFileTask extends Task<FileDigestResult, FileProgress> {
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
		public FileDigestResult execute() throws Exception {
			return transfer(fileId, remoteHost, file, fileSize);
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
				while (!isCancelled() && (readLength = input.read(buffer, 0, BUFFER_LENGTH)) != -1) {
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
	}
}
