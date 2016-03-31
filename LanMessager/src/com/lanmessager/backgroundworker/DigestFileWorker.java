package com.lanmessager.backgroundworker;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import com.lanmessager.backgroundworker.process.Report;
import com.lanmessager.backgroundworker.process.ResultReport;
import com.lanmessager.backgroundworker.process.StatusReport;
import com.lanmessager.file.FileDigestCalculator;
import com.lanmessager.file.FileDigestResult;

public class DigestFileWorker {
	private static final Logger LOGGER = Logger.getLogger(DigestFileWorker.class.getSimpleName());

	private final FileDigestCalculator calculator;

	private final DigestFileMonitorSwingWorker monitor;
	
	private final Object monitorLock;

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
		this.monitorLock = new Object();
		this.monitor = new DigestFileMonitorSwingWorker();
		this.monitor.execute();
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
		synchronized (monitorLock) {
			monitorLock.notify();
		}
	}

	public void cancel(String fileId) {
		calculator.cancel(fileId);
	}
	
	public void shutdown() {
		calculator.shutdown();
	}

	protected void onDone(String fileId, ResultReport<String, FileDigestResult> result) {
		if (completedListeners != null) {
			FileCompletedEvent event = new FileCompletedEvent(this);
			event.setFileId(fileId);

			if (result.isCancelled()) {
				event.setCancelled(true);
			} else {
				try {
					event.setFileDigestResult(result.getResult());
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

	private class DigestFileMonitorSwingWorker extends SwingWorker<Void, Report<String>> {
		private static final int REPORT_TIME_INTERVAL = 1000;

		@Override
		protected Void doInBackground() throws Exception {
			LOGGER.info("Background thread starts.");

			while (true) {
				if (calculator.isIdle()) {
					// Block monitor thread if calculator is idle.
					synchronized (monitorLock) {
						LOGGER.info("Background thread sleeps.");
						monitorLock.wait();
						LOGGER.info("Background thread wakes up.");
					}
				}
				
				LOGGER.debug("Background thread is working.");
				
				List<Report<String>> reportList = calculator.report();
				@SuppressWarnings("unchecked")
				Report<String>[] reports = new Report[reportList.size()];
				reportList.toArray(reports);
				publish(reports);

				Thread.sleep(REPORT_TIME_INTERVAL);
			}
		}

		@Override
		protected void process(List<Report<String>> chunks) {
			super.process(chunks);

			chunks.forEach(report -> {
				if (report instanceof ResultReport) {
					@SuppressWarnings("unchecked")
					ResultReport<String, FileDigestResult> resultReport = (ResultReport<String, FileDigestResult>) report;
					onDone(resultReport.getKey(), resultReport);
				} else if (report instanceof StatusReport) {
					@SuppressWarnings("unchecked")
					StatusReport<String, FileProgress> statusReport = (StatusReport<String, FileProgress>) report;
					String fileId = statusReport.getKey();
					FileProgress progress = statusReport.getStatus();
					onProgressUpdated(fileId, progress.getProcessed(), progress.getTotal());
				} else {

				}
			});
		}

		@Override
		protected void done() {
			super.done();

			LOGGER.info("Background thread exits.");
		}
	}
}
