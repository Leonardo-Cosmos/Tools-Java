package com.lanmessager.backgroundworker;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import com.lanmessager.communication.ProgressUpdatedEvent;
import com.lanmessager.communication.TransferFileServer;
import com.lanmessager.file.FileDigestResult;

public class FileReceiveWorker {
	private static final Logger LOGGER = Logger.getLogger(FileReceiveWorker.class.getSimpleName());

	private TransferFileServerSwingWorker worker;
	
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

	public FileReceiveWorker(String fileId) {
		this.fileId = fileId;
	}
	
	public void launch(int port, File file, long fileSize) {
		worker = new TransferFileServerSwingWorker(port, file, fileSize);
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

	private class TransferFileServerSwingWorker extends SwingWorker<FileDigestResult, ProgressUpdatedEvent> {
		private int port;
		private File file;
		private long fileSize;
		private TransferFileServer server;

		public TransferFileServerSwingWorker(int port, File file, long fileSize) {
			this.port = port;
			this.file = file;
			this.fileSize = fileSize;
		}

		public void cancel() {
			if (server != null) {
				server.cancel();
			}
		}
		
		@Override
		protected FileDigestResult doInBackground() throws Exception {
			server = new TransferFileServer();
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
