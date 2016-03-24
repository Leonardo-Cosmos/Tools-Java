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

import com.lanmessager.file.FileDigestCalculator;
import com.lanmessager.file.FileDigestResult;

public class DigestFileWorker {
	private static final Logger LOGGER = Logger.getLogger(DigestFileWorker.class.getSimpleName());
	
	private Map<String, DigestFileSwingWorker> workerMap;
	
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
	
	public DigestFileWorker() {
		workerMap = new HashMap<>();
	}
	
	/**
	 * Schedule a work thread to calculate file digest.
	 * Every invoking will create a new work thread.
	 * 
	 * @param fileId
	 * @param file
	 */
	public void digest(String fileId, File file) {
		DigestFileSwingWorker worker = new DigestFileSwingWorker(fileId, file);
		
		workerMap.put(fileId, worker);
		
		worker.execute();
	}
	
	public void cancel(String fileId) {
		if (!workerMap.containsKey(fileId)) {
			LOGGER.warn("Cannot find file digest worker. File ID: " + fileId);
			return;
		}
		
		DigestFileSwingWorker worker = workerMap.get(fileId);
		worker.cancel();
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
	
	protected void onCompletedByCancellation(String fileId) {
		if (completedListeners != null) {
			FileCompletedEvent event = new FileCompletedEvent(this);
			event.setFileId(fileId);
			event.setCanceled(true);
			for (FileCompletedListener listener : completedListeners) {
				listener.complete(event);
			}
		}
	}
	
	protected void onCompletedByFailure(String fileId, Exception exception) {
		if (completedListeners != null) {
			FileCompletedEvent event = new FileCompletedEvent(this);
			event.setFileId(fileId);
			event.setFailed(true);
			event.setCause(exception);
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
	
	private class DigestFileSwingWorker extends SwingWorker<FileDigestResult, Long> {
		private String fileId;
		private File file;
		private long fileLength;
		private FileDigestCalculator calculator;
		
		public DigestFileSwingWorker(String fileId, File file) {
			this.fileId = fileId;
			this.file = file;
			this.fileLength = file.length();
		}

		public void cancel() {
			if (calculator != null) {
				calculator.cancel();
			}
		}
		
		@Override
		protected FileDigestResult doInBackground() throws Exception {
			LOGGER.info("Digest file background thread starts.");
			
			calculator = new FileDigestCalculator();
			calculator.addProgressUpdatedListener(event -> {
				publish(event.getProcessed());
			});
			FileDigestResult result = calculator.digestFile(file);
			
			LOGGER.info("Digest file background thread exits.");
			return result;
		}
		
		@Override
		protected void process(List<Long> chunks) {
			super.process(chunks);
			
			long updatedLength = chunks.get(0);
			onProgressUpdated(fileId, updatedLength, fileLength);
		}
		
		@Override
		protected void done() {
			super.done();
			
			workerMap.remove(fileId);
			
			if (calculator.isCanceled()) {
				onCompletedByCancellation(fileId);
			} else {				
				try {
					onCompleted(fileId, get());
				} catch (InterruptedException | ExecutionException ex) {
					onCompletedByFailure(fileId, ex);
					LOGGER.error("Digest file background thread failed.", ex);
				}				
			}
		}
	}
}
