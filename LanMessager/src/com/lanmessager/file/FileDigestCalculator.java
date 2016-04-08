package com.lanmessager.file;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
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

import sun.awt.AppContext;

public class FileDigestCalculator extends TaskExecutor<String, FileDigestResult, FileProgress> {
	private static final Logger LOGGER = Logger.getLogger(FileDigestCalculator.class.getSimpleName());

	private static final String CLASS_NAME = FileDigestCalculator.class.getSimpleName();
	
	private static final int BUFFER_LENGTH = 0x1000;

	private static final int THREAD_NUMBER = 3;
	
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

	public FileDigestCalculator() {
		super(getWorkersExecutorService());		
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
