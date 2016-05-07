package com.lanmessager.backgroundworker;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.lanmessager.communication.TransferFileServer;
import com.lanmessager.concurrent.ResultReport;
import com.lanmessager.concurrent.StatusReport;
import com.lanmessager.concurrent.SwingMonitor;
import com.lanmessager.concurrent.TaskExecutor;
import com.lanmessager.file.FileDigestResult;

public class FileReceiveWorker {
	private static final Logger LOGGER = Logger.getLogger(FileReceiveWorker.class.getSimpleName());

	private final TransferFileServer server;

	private Future<Void> serverResult;

	private final ReceiveFileMonitor monitor;

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
		server = new TransferFileServer();
		monitor = new ReceiveFileMonitor(server);
		monitor.execute();
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

	public void receive(String fileId, File file, long fileSize) {
		server.receive(fileId, file, fileSize);
	}

	public void cancel(String fileId) {
		server.cancel(fileId);
	}
	
	public void shutdown() {
		monitor.shutdown();
		server.shutdown();
	}

	private class ReceiveFileMonitor extends SwingMonitor<String, FileDigestResult, FileProgress> {

		public ReceiveFileMonitor(TaskExecutor<String, FileDigestResult, FileProgress> executor) {
			super(executor);
		}

		@Override
		protected void onDone(ResultReport<String, FileDigestResult> report) {
			String fileId = report.getKey();

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
}
