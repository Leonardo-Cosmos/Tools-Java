package com.lanmessager.backgroundworker;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.lanmessager.communication.TransferFileServer;
import com.lanmessager.concurrent.ResultReport;
import com.lanmessager.concurrent.StatusReport;
import com.lanmessager.concurrent.SwingMonitor;
import com.lanmessager.concurrent.TaskExecutor;
import com.lanmessager.file.FileDigestResult;

public class FileReceiveWorker {
	private static final Logger LOGGER = Logger.getLogger(FileReceiveWorker.class.getSimpleName());

	private final Map<String, ReceiveFileTask> receiveFileTaskMap;

	private final TransferFileServer server;

	private Future<Void> serverResult;

	private final ReceiveFileMonitor monitor;

	private Set<FileCompletedListener> completedListeners;

	private Set<FileProgressUpdatedListener> progressUpdatedListeners;

	public void addCompletedListener(FileCompletedListener listener) {
		if (completedListeners == null) {
			completedListeners = new HashSet<>();
		}
		completedListeners.add(listener);
	}

	public void removeCompletedListener(FileCompletedListener listener) {
		if (completedListeners != null) {
			completedListeners.remove(listener);
		}
	}

	public void addProgressUpdatedListeners(FileProgressUpdatedListener listener) {
		if (progressUpdatedListeners == null) {
			progressUpdatedListeners = new HashSet<>();
		}
		progressUpdatedListeners.add(listener);
	}

	public void removeProgressUpdatedListeners(FileProgressUpdatedListener listener) {
		if (progressUpdatedListeners != null) {
			progressUpdatedListeners.remove(listener);
		}
	}

	public FileReceiveWorker() {
		this.receiveFileTaskMap = new HashMap<>();
		this.server = new TransferFileServer();
		this.monitor = new ReceiveFileMonitor(server);
	}

	public void startReceiveServer() {
		Callable<Void> serverTask = (() -> {
			server.start();
			return null;
		});
		ExecutorService executor = Executors.newSingleThreadExecutor();
		serverResult = executor.submit(serverTask);
	}

	public void stopReceiveServer() {
		server.stop();
	}

	public void checkReceiveServer() throws Exception {
		if (serverResult.isDone()) {
			try {
				serverResult.get();
			} catch (InterruptedException ex) {

			} catch (ExecutionException ex) {
				throw (Exception) ex.getCause();
			}
		}
	}

	public void register(String fileId, File file, long fileSize) {
		if (receiveFileTaskMap.containsKey(fileId)) {
			LOGGER.warn("Duplicated task: " + fileId);
			return;
		}

		ReceiveFileTask task = new ReceiveFileTask();
		task.setFile(file);
		task.setFileSize(fileSize);
		receiveFileTaskMap.put(fileId, task);
	}

	public void unregister(String fileId) {
		if (!receiveFileTaskMap.containsKey(fileId)) {
			LOGGER.info("Cannot find task " + fileId);
			return;
		}
		receiveFileTaskMap.remove(fileId);
	}

	public void receive(String fileId) {
		if (!receiveFileTaskMap.containsKey(fileId)) {
			LOGGER.warn("Cannot file receive file task " + fileId);
			return;
		}
		ReceiveFileTask task = receiveFileTaskMap.get(fileId);
		server.receive(fileId, task.getFile(), task.getFileSize());
	}

	public void cancel(String fileId) {
		server.cancel(fileId);
	}

	private class ReceiveFileMonitor extends SwingMonitor<String, FileDigestResult, FileProgress> {

		public ReceiveFileMonitor(TaskExecutor<String, FileDigestResult, FileProgress> executor) {
			super(executor);
		}

		@Override
		protected void onDone(ResultReport<String, FileDigestResult> report) {
			String fileId = report.getKey();
			
			if (!receiveFileTaskMap.containsKey(fileId)) {
				LOGGER.warn("Cannot find send file task " + fileId);
				return;
			}
			receiveFileTaskMap.remove(fileId);

			if (completedListeners != null) {
				FileCompletedEvent event = new FileCompletedEvent(this);
				event.setFileId(fileId);

				if (report.isCancelled()) {
					event.setCancelled(true);
				} else {
					try {
						event.setFileDigestResult(report.getResult());
					} catch (CancellationException ex) {
						LOGGER.info("Task is cancelled: " + fileId);
						return;
					} catch (InterruptedException ex) {
						LOGGER.info("Task is interrupted: " + fileId);
						return;
					} catch (ExecutionException ex) {
						event.setFailed(true);
						event.setCause(ex.getCause());
					} catch (Exception ex) {
						event.setFailed(true);
						event.setCause(ex);
					}
				}

				for (FileCompletedListener listener : completedListeners) {
					listener.complete(event);
				}
			}
		}

		@Override
		protected void onStatusUpdated(StatusReport<String, FileProgress> report) {
			if (progressUpdatedListeners != null) {
				FileProgressUpdatedEvent event = new FileProgressUpdatedEvent(this);
				event.setFileId(report.getKey());
				event.setProcessed(report.getStatus().getProcessed());
				event.setTotal(report.getStatus().getTotal());
				for (FileProgressUpdatedListener listener : progressUpdatedListeners) {
					listener.updateProgress(event);
				}
			}
		}
		
	}

	private class ReceiveFileTask {

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
}
