package com.lanmessager.backgroundworker;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;

import com.lanmessager.communication.TransferFileClient;
import com.lanmessager.concurrent.ResultReport;
import com.lanmessager.concurrent.StatusReport;
import com.lanmessager.concurrent.SwingMonitor;
import com.lanmessager.concurrent.TaskExecutor;
import com.lanmessager.file.FileDigestResult;

public class FileSendWorker {
	private static final Logger LOGGER = Logger.getLogger(FileSendWorker.class.getSimpleName());

	private final Map<String, SendFileTask> sendFileTaskMap;

	private final TransferFileClient client;

	private final SendFileMonitor monitor;

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

	public FileSendWorker() {
		sendFileTaskMap = new HashMap<>();
		client = new TransferFileClient();
		monitor = new SendFileMonitor(client);
		monitor.execute();
	}

	public void register(String receiverAddress, String fileId, File file, long fileSize) {
		if (sendFileTaskMap.containsKey(fileId)) {
			LOGGER.warn("Duplicated task: " + fileId);
			return;
		}

		SendFileTask task = new SendFileTask();
		task.setFile(file);
		task.setFileSize(fileSize);
		task.setAddress(receiverAddress);
		sendFileTaskMap.put(fileId, task);
	}

	public void unregister(String fileId) {
		if (!sendFileTaskMap.containsKey(fileId)) {
			LOGGER.info("Cannot find task " + fileId);
			return;
		}
		sendFileTaskMap.remove(fileId);
	}

	public void send(String fileId) {
		if (!sendFileTaskMap.containsKey(fileId)) {
			LOGGER.warn("Cannot find send file task " + fileId);
			return;
		}
		SendFileTask task = sendFileTaskMap.get(fileId);
		client.send(fileId, task.getAddress(), task.getFile(), task.getFileSize());
	}

	public void cancel(String fileId) {
		client.cancel(fileId);
	}
	
	public void shutdown() {
		monitor.shutdown();
		client.shutdown();
	}

	private class SendFileMonitor extends SwingMonitor<String, FileDigestResult, FileProgress> {

		public SendFileMonitor(TaskExecutor<String, FileDigestResult, FileProgress> executor) {
			super(executor);
		}

		@Override
		protected void onDone(ResultReport<String, FileDigestResult> report) {
			String fileId = report.getKey();
			
			if (!sendFileTaskMap.containsKey(fileId)) {
				LOGGER.warn("Cannot find send file task " + fileId);
				return;
			}
			sendFileTaskMap.remove(fileId);

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
						LOGGER.error("Task is failed: " + fileId, ex);
						event.setFailed(true);
						event.setCause(ex.getCause());
					} catch (Exception ex) {
						LOGGER.error("Task is failed: " + fileId, ex);
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

	private class SendFileTask {

		private File file;

		private long fileSize;

		private String receiverAddress;

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

		public String getAddress() {
			return receiverAddress;
		}

		public void setAddress(String address) {
			this.receiverAddress = address;
		}
	}
}
