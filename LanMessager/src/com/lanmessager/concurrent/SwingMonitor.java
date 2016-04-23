package com.lanmessager.concurrent;

import java.util.List;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

/**
 * Implements mechanism of interaction with Swing for {@link Monitor}. 
 *
 * @param <K> The key type used to retrieve {@link Task}.
 * @param <V> The result type returned by {@link Task}.
 * @param <S> The status type updated by {@link Task}.
 */
public abstract class SwingMonitor<K, V, S> extends Monitor<K, V, S> {
	private static final Logger LOGGER = Logger.getLogger(SwingMonitor.class.getSimpleName());
	
	private static final int REPORT_TIME_INTERVAL = 1000;
	
	private final MonitorSwingWorker worker;
	
	private TaskExecutor<K, V, S> executor;
	
	public SwingMonitor(TaskExecutor<K, V, S> executor) {
		this.executor = executor;
		this.worker = new MonitorSwingWorker();
		
		executor.setMonitor(this);
	}
	
	public void execute() {
		worker.execute();
	}
	
	public void shutdown() {
		worker.shutdown();
	}
	
	@Override
	void wakeup() {
		worker.wakeup();
	}
	
	private class MonitorSwingWorker extends SwingWorker<Void, Report<K>> {
		
		private final Object monitorLock  = new Object();
		
		private boolean isShutdown = false;
		
		public void shutdown() {
			isShutdown = true;
			wakeup();
		}
		
		public void wakeup() {
			synchronized (monitorLock) {
				monitorLock.notify();
			}
		}
		
		@Override
		protected Void doInBackground() throws Exception {
			LOGGER.info("Monitor thread starts.");

			while (!isShutdown) {
				if (executor.isIdle()) {
					// Block monitor thread if processor is idle.
					synchronized (monitorLock) {
						LOGGER.info("Monitor thread sleeps.");
						monitorLock.wait();
						LOGGER.info("Monitor thread wakes up.");
					}
				}

				LOGGER.debug("Monitor thread is working.");

				List<Report<K>> reportList = executor.report();
				@SuppressWarnings("unchecked")
				Report<K>[] reports = new Report[reportList.size()];
				reportList.toArray(reports);
				publish(reports);

				Thread.sleep(REPORT_TIME_INTERVAL);
			}
			return null;
		}

		@Override
		protected void process(List<Report<K>> chunks) {
			super.process(chunks);

			chunks.forEach(report -> {
				if (report instanceof ResultReport) {
					@SuppressWarnings("unchecked")
					ResultReport<K, V> resultReport = (ResultReport<K, V>) report;
					onDone(resultReport);
				} else if (report instanceof StatusReport) {
					@SuppressWarnings("unchecked")
					StatusReport<K, S> statusReport = (StatusReport<K, S>) report;
					onStatusUpdated(statusReport);
				} else {

				}
			});
		}

		@Override
		protected void done() {
			super.done();

			LOGGER.info("Monitor thread exits.");
			try {
				get();
			} catch (Exception ex) {
				LOGGER.error("Monitor thread exits with exception.", ex);
			}
		}
	}
}
