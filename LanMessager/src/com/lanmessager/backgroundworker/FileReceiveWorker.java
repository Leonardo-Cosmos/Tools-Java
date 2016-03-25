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
import com.lanmessager.communication.TransferFileServer;
import com.lanmessager.file.FileDigestResult;
import com.lanmessager.module.ReceiveFileTask;

public class FileReceiveWorker {
	private static final Logger LOGGER = Logger.getLogger(FileReceiveWorker.class.getSimpleName());

	private Map<String, ReceiveFileTask> receiveFileTaskMap;
	
	private final TransferFileServer server;
	
	private final TransferFileServerSwingWorker worker;
	
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
		this.worker = new TransferFileServerSwingWorker();
	}
	
	public void start() {
		worker.execute();
	}
	
	public void stop() {
		
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
		if (!receiveFileTaskMap.containsKey(fileId)) {
			LOGGER.warn("Cannot find receive file task " + fileId);
			return;
		}
		receiveFileTaskMap.remove(fileId);
		
		server.cancel(fileId);
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

	private class TransferFileServerSwingWorker extends SwingWorker<FileDigestResult, ProgressUpdatedEvent> {
		private int port;
		private String fileId;
		private File file;
		private long fileSize;
		private TransferFileServer server;

		public TransferFileServerSwingWorker() {

		}
		
		@Override
		protected FileDigestResult doInBackground() throws Exception {
			server.addProgressUpdatedListener(event -> publish(event));

			LOGGER.info("File receiver background thread starts.");
			server.start(port, file, fileSize);

			LOGGER.info("File receiver background thread exits.");
			return server.getFileDigestResult();
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

			receiveFileTaskMap.remove(fileId);
			
			try {
				onCompleted(fileId, get());				
			} catch (InterruptedException | ExecutionException ex) {
				onCompletedWithException(fileId, ex);
			}
		}
	}
}
