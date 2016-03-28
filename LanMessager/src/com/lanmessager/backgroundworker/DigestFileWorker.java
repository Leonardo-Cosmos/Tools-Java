package com.lanmessager.backgroundworker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import com.lanmessager.file.FileDigestCalculator;
import com.lanmessager.file.FileDigestResult;

public class DigestFileWorker {
	private static final Logger LOGGER = Logger.getLogger(DigestFileWorker.class.getSimpleName());
		
	private final FileDigestCalculator calculator;
	
	private final DigestFileMonitorSwingWorker monitor;
	
	private volatile boolean isMonitorShutdown = false;
	
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
		this.monitor = new DigestFileMonitorSwingWorker();
		this.monitor.execute();
	}
	
	/**
	 * Schedule a work thread to calculate file digest.
	 * Every invoking will create a new work thread.
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
		
	protected void onTaskDone(String fileId, Future<FileDigestResult> result) {	
		if (completedListeners != null) {
			FileCompletedEvent event = new FileCompletedEvent(this);
			event.setFileId(fileId);
			
			if (!result.isDone()) {
				LOGGER.warn("An unfinished task is reported: " + fileId);
				return;
			}
			
			if (result.isCancelled()) {
				event.setCanceled(true);
			} else {
				try {
					event.setFileDigestResult(result.get());
				} catch (CancellationException ex) {
					LOGGER.info("Task is canceled: " + fileId);
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
	
	private class DigestFileMonitorSwingWorker extends SwingWorker<Void, FileReport> {
		private static final int REPORT_TIME_INTERVAL = 1000;
		
		@Override
		protected Void doInBackground() throws Exception {
			LOGGER.info("Digest file background thread starts.");
			
			while (!isMonitorShutdown) {
				LOGGER.debug("Digest file background thread is monitoring.");
				Map<String, Future<FileDigestResult>> resultMap = calculator.reportResult();
				Map<String, FileProgress> progressMap = calculator.reportProgress();
				
				if (resultMap.size() > 0 || progressMap.size() > 0) {
					List<FileReport> reportList = new ArrayList<>(resultMap.size() + progressMap.size()); 
					
					if (resultMap.size() > 0) {
						resultMap.forEach((fileId, result) -> {
							FileResultReport report = new FileResultReport();
							report.setFileId(fileId);
							report.setResult(result);
							reportList.add(report);
						});
					}
					
					if (progressMap.size() > 0) {
						progressMap.forEach((fileId, progress) -> {
							FileProgressReport report = new FileProgressReport();
							report.setFileId(fileId);
							report.setProgress(progress);
							reportList.add(report);
						});
					}
					
					FileReport[] reports = new FileReport[reportList.size()];
					reportList.toArray(reports);
					publish(reports);
				}
				
				Thread.sleep(REPORT_TIME_INTERVAL);
			}			
			
			return null;
		}
		
		@Override
		protected void process(List<FileReport> chunks) {
			super.process(chunks);
			
			chunks.forEach(report -> {
				if (report instanceof FileResultReport) {
					FileResultReport resultReport = (FileResultReport) report;
					onTaskDone(resultReport.getFileId(), resultReport.getResult());
				} else if (report instanceof FileProgressReport) {
					FileProgressReport progressReport = (FileProgressReport) report;
					String fileId = progressReport.getFileId();				
					FileProgress progress = progressReport.getProgress();
					onProgressUpdated(fileId, progress.getProcessed(), progress.getTotal());
				} else {
					
				}
			});
		}
		
		@Override
		protected void done() {
			super.done();
			
			LOGGER.info("Digest file background thread exits.");
		}
	}
}
