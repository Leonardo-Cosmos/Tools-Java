package com.lanmessager.backgroundworker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import com.lanmessager.communication.TransferFileServer;
import com.lanmessager.file.FileDigestResult;

public class FileReceiveWorker {
	private static final Logger LOGGER = Logger.getLogger(FileReceiveWorker.class.getSimpleName());

	private final Map<String, ReceiveFileTask> receiveFileTaskMap;

	private final TransferFileServer server;

	private Future<Void> serverResult;

	private final ReceiveFileMonitorSwingWorker monitor;

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

	public FileReceiveWorker() {
		this.receiveFileTaskMap = new HashMap<>();
		this.server = new TransferFileServer();
		this.monitorLock = new Object();
		this.monitor = new ReceiveFileMonitorSwingWorker();
		this.monitor.execute();
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
		
		synchronized (monitorLock) {
			monitorLock.notify();
		}
	}

	public void cancel(String fileId) {
		server.cancel(fileId);
	}

	protected void onDone(String fileId, Future<FileDigestResult> result) {
		if (!receiveFileTaskMap.containsKey(fileId)) {
			LOGGER.warn("Cannot find send file task " + fileId);
			return;
		}
		receiveFileTaskMap.remove(fileId);

		if (completedListeners != null) {
			FileCompletedEvent event = new FileCompletedEvent(this);
			event.setFileId(fileId);

			if (!result.isDone()) {
				LOGGER.warn("An unfinished task is reported: " + fileId);
				return;
			}

			if (result.isCancelled()) {
				event.setCancelled(true);
			} else {
				try {
					event.setFileDigestResult(result.get());
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

	private class ReceiveFileMonitorSwingWorker extends SwingWorker<Void, FileReport> {
		private static final int REPORT_TIME_INTERVAL = 1000;

		@Override
		protected Void doInBackground() throws Exception {
			LOGGER.info("Background thread starts.");

			while (true) {
				if (server.isIdle()) {
					// Block monitor thread if calculator is idle.
					synchronized (monitorLock) {
						LOGGER.info("Background thread sleeps.");
						monitorLock.wait();
						LOGGER.info("Background thread wakes up.");
					}
				}

				LOGGER.debug("Background thread is working.");
				
				Map<String, FileProgress> progressMap = server.reportProgress();
				Map<String, Future<FileDigestResult>> resultMap = server.reportResult();
				
				List<FileReport> reportList = new ArrayList<>(resultMap.size() + progressMap.size());
				
				if (progressMap.size() > 0) {
					progressMap.forEach((fileId, progress) -> {
						FileProgressReport report = new FileProgressReport();
						report.setFileId(fileId);
						report.setProgress(progress);
						reportList.add(report);
					});
				}

				if (resultMap.size() > 0) {
					resultMap.forEach((fileId, result) -> {
						FileResultReport report = new FileResultReport();
						report.setFileId(fileId);
						report.setResult(result);
						reportList.add(report);
					});
				}

				FileReport[] reports = new FileReport[reportList.size()];
				reportList.toArray(reports);
				publish(reports);

				Thread.sleep(REPORT_TIME_INTERVAL);
			}
		}

		@Override
		protected void process(List<FileReport> chunks) {
			super.process(chunks);

			chunks.forEach(report -> {
				if (report instanceof FileResultReport) {
					FileResultReport resultReport = (FileResultReport) report;
					onDone(resultReport.getFileId(), resultReport.getResult());
				} else if (report instanceof FileProgressReport) {
					FileProgressReport progressReport = (FileProgressReport) report;
					String fileId = progressReport.getFileId();
					if (!receiveFileTaskMap.containsKey(fileId)) {
						LOGGER.info("Cannot find task " + fileId);
						return;
					}
					FileProgress progress = progressReport.getProgress();
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
