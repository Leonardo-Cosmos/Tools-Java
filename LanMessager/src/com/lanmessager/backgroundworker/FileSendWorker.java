package com.lanmessager.backgroundworker;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import com.lanmessager.communication.ProgressUpdatedEvent;
import com.lanmessager.communication.TransferFileClient;
import com.lanmessager.file.FileDigestResult;

public class FileSendWorker {
	private static final Logger LOGGER = Logger.getLogger(FileSendWorker.class.getSimpleName());

	private TransferFileClientSwingWorker worker;
	
	private final String fileId;
	
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
	
	public FileSendWorker(String fileId) {
		this.fileId = fileId;
	}
	
	public void send(String remoteHost, int port, File file, long fileSize) {
		worker = new TransferFileClientSwingWorker(remoteHost, port, file, fileSize);
		worker.execute();
	}
	
	public void cancel() {
		if (worker != null) {
			worker.cancel();
		}
	}

	protected void onCompleted(FileDigestResult fileDigestResult) {
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
	
	protected void onCompletedWithException(Exception exception) {
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

	protected void onProgressUpdated(long processed, long total) {
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
		private File file;
		private long fileSize;
		private TransferFileClient client;

		public TransferFileClientSwingWorker(String remoteHost, int port, File file, long fileSize) {
			this.remoteHost = remoteHost;
			this.port = port;
			this.file = file;
			this.fileSize = fileSize;
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
			onProgressUpdated(event.getProcessed(), event.getTotal());
		}

		@Override
		protected void done() {
			super.done();
			
			try {
				onCompleted(get());
			} catch (InterruptedException | ExecutionException ex) {
				onCompletedWithException(ex);
			}
		}
	}
}
