package com.lanmessager.backgroundworker;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import com.lanmessager.communication.ProgressUpdatedEvent;
import com.lanmessager.communication.TransferFileClient;
import com.lanmessager.communication.TransferFileServer;
import com.lanmessager.file.FileDigestResult;
import com.lanmessager.module.SendFileTask;

public class FileSendWorker {
	private static final Logger LOGGER = Logger.getLogger(FileSendWorker.class.getSimpleName());

	private Map<String, SendFileTask> sendFileTaskMap;
	
	private final TransferFileClient client;
	
	private final TransferFileClientSwingWorker worker;
	
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
		this.sendFileTaskMap = new HashMap<>();
		this.client = new TransferFileClient();
		this.worker = new TransferFileClientSwingWorker();
	}
	
	public void start() {
		worker.execute();
	}
	
	public void stop() {
		
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
		if (!sendFileTaskMap.containsKey(fileId)) {
			LOGGER.warn("Cannot find send file task " + fileId);
			return;
		}		
		sendFileTaskMap.remove(fileId);
		
		client.cancel(fileId);
	}

	protected void onCompleted(String fileId, FileDigestResult fileDigestResult) {
		if (completedListeners != null) {
			FileCompletedEvent event = new FileCompletedEvent(this);
			event.setFileId(fileId);
			event.setFailed(false);
			event.setFileDigestResult(fileDigestResult);
			for (FileCompletedListener listener : completedListeners) {
				listener.complete(event);
			}
		}
	}
	
	protected void onCompletedWithException(String fileId, Exception exception) {
		if (completedListeners != null) {
			Throwable cause = null;
			if (exception instanceof ExecutionException) {
				exception.getCause();				
			} else {
				cause = exception;
			}
			
			FileCompletedEvent event = new FileCompletedEvent(this);
			event.setFileId(fileId);
			event.setFailed(true);
			event.setCause(cause);
			for (FileCompletedListener listener : completedListeners) {
				listener.complete(event);
			}
		}
	}

	protected void onProgressUpdated(String fileId, long processed, long total) {
		if (progressUpdatedListeners != null) {
			FileProgressUpdatedEvent event = new FileProgressUpdatedEvent(this);
			event.setFileId(fileId);
			event.setProcessed(processed);
			event.setTotal(total);
			for (FileProgressUpdatedListener listener : progressUpdatedListeners) {
				listener.updateProgress(event);
			}
		}
	}

	private class TransferFileClientSwingWorker extends SwingWorker<FileDigestResult, ProgressUpdatedEvent> {
		private String remoteHost;
		private int port;
		private String fileId;
		private File file;
		private long fileSize;
		private TransferFileClient client;

		public TransferFileClientSwingWorker() {
			/*this.remoteHost = remoteHost;
			this.port = port;
			this.fileId = fileId;
			this.file = file;
			this.fileSize = fileSize;*/
		}

		public void cancel() {
			if (client != null) {
				client.cancel();
			}
		}
		
		@Override
		protected FileDigestResult doInBackground() throws Exception {
			client = new TransferFileClient();
			client.addProgressUpdatedListener(event -> publish(event));

			LOGGER.info("File sender background thread starts.");
			client.send(remoteHost, port, file, fileSize);

			LOGGER.info("File sender background thread exits.");
			return client.getFileDigestResult();
		}

		@Override
		protected void process(List<ProgressUpdatedEvent> chunks) {
			super.process(chunks);

			ProgressUpdatedEvent event = chunks.get(0);
			onProgressUpdated(fileId, event.getProcessed(), event.getTotal());
		}

		@Override
		protected void done() {
			super.done();
			
			sendFileTaskMap.remove(fileId);
			
			try {
				onCompleted(fileId, get());
			} catch (InterruptedException | ExecutionException ex) {
				onCompletedWithException(fileId, ex);
			}
		}
	}
}
