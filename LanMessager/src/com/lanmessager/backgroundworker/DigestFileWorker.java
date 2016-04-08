package com.lanmessager.backgroundworker;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;

import com.lanmessager.concurrent.ResultReport;
import com.lanmessager.concurrent.StatusReport;
import com.lanmessager.concurrent.SwingMonitor;
import com.lanmessager.concurrent.TaskExecutor;
import com.lanmessager.file.FileDigestCalculator;
import com.lanmessager.file.FileDigestResult;

public class DigestFileWorker {
	private static final Logger LOGGER = Logger.getLogger(DigestFileWorker.class.getSimpleName());

	private final FileDigestCalculator calculator;

	private final DigestFileMonitor monitor;

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
		this.calculator = new FileDigestCalculator();
		this.monitor = new DigestFileMonitor(calculator);
	}

	/**
	 * Schedule a work thread to calculate file digest. Every invoking will
	 * create a new work thread.
	 * 
	 * @param fileId
	 * @param file
	 */
	public void digest(String fileId, File file) {
		calculator.digestFile(fileId, file);
	}

	public void cancel(String fileId) {
		calculator.cancel(fileId);
	}
	
	public void shutdown() {
		calculator.shutdown();
	}

	private class DigestFileMonitor extends SwingMonitor<String, FileDigestResult, FileProgress> {

		public DigestFileMonitor(TaskExecutor<String, FileDigestResult, FileProgress> executor) {
			super(executor);
		}

		@Override
		protected void onDone(ResultReport<String, FileDigestResult> report) {
			if (completedListeners != null) {
				String fileId = report.getKey();
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
}
