package com.lanmessager.concurrent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import sun.awt.AppContext;

/**
 * An TaskExecutor is an wrap of {@link ExecutorService} that accept {@link Task} only.
 * All {@link Task} can be retrieved by a key.
 * Every task can report status during execution, and report result when execution is done.
 * Report will be merged to a collection so that it will be convenient to display in UI.
 *
 * @param <K> The key type used to retrieve {@link Task}.
 * @param <V> The result type returned by {@link Task}.
 * @param <S> The status type updated by {@link Task}.
 */
public class TaskExecutor<K, V, S> {
	private static final Logger LOGGER = Logger.getLogger(TaskExecutor.class.getSimpleName());
	
	private static final int DEFAULT_THREAD_NUMBER = 3;
	
	private final ExecutorService executorService;
	
	private final Map<K, Submission<V, S>> submissionMap;
	
	private Monitor<K, V, S> monitor;
	
	private static ExecutorService getWorkersExecutorService(Class<? extends TaskExecutor<?, ?, ?>> clazz,
			int threadNumber) {
		LOGGER.debug("Class name: " + clazz.getSimpleName());
		
		final AppContext appContext = AppContext.getAppContext();
		ExecutorService executorService = (ExecutorService) appContext.get(clazz);
		if (executorService == null) {
			ThreadFactory threadFactory = new ThreadFactory() {
				final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
				@Override
				public Thread newThread(Runnable r) {
					Thread thread = defaultFactory.newThread(r);
					thread.setName(clazz.getSimpleName() + "-" + thread.getName());
					thread.setDaemon(true);
					return thread;
				}
			};
			executorService = new ThreadPoolExecutor(threadNumber, threadNumber, 1L, TimeUnit.MINUTES,
					new LinkedBlockingQueue<>(), threadFactory);
			appContext.put(clazz, executorService);
		}
		
		final ExecutorService es = executorService;
		appContext.addPropertyChangeListener(AppContext.DISPOSED_PROPERTY_NAME,
				new PropertyChangeListener() {
					
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						boolean disposed = (Boolean) evt.getNewValue();
						if (disposed) {
							final WeakReference<ExecutorService> executorServiceRef = new WeakReference<ExecutorService>(es);
							final ExecutorService executorService = executorServiceRef.get();
							if (executorService != null) {
								AccessController.doPrivileged(new PrivilegedAction<Void>() {
									public Void run() {
										executorService.shutdown();
										return null;
									};
								});
							}
						}
					}
				});

		return executorService;
	}
	
	void setMonitor(Monitor<K, V, S> monitor) {
		this.monitor = monitor;
	}

	@SuppressWarnings("unchecked")
	public TaskExecutor(int threadNumber) {
		this.executorService = getWorkersExecutorService(
				(Class<? extends TaskExecutor<?, ?, ?>>) this.getClass(),
				threadNumber);
		this.submissionMap = new HashMap<>();
	}
	
	public TaskExecutor() {
		this(DEFAULT_THREAD_NUMBER);
	}
	
	public void submit(K key, Task<V, S> task) {
		LOGGER.debug("New task: " + key);
		synchronized (submissionMap) {
			task.setStatus(new FinalStatus<>());
			Future<V> result = executorService.submit(task);
			if (submissionMap.containsKey(key)) {
				LOGGER.warn("Duplicated task: " + key);
			}			
			submissionMap.put(key, new Submission<>(task, result));
		}
		if (null != monitor) {
			monitor.wakeup();
		}
	}
	
	public void cancel(K key) {
		Submission<V, S> submission = null;
		synchronized (submissionMap) {
			if (!submissionMap.containsKey(key)) {
				LOGGER.warn("Task doesn't exist: " + key);
				return;
			}
			submission = submissionMap.get(key);
		}
		cancel(key, submission);
	}
	
	private void cancel(K key, Submission<V, S> submission) {
		Future<V> result = submission.getResult();
		Task<V, S> task = submission.getTask();
		if (result != null) {
			// Try to cancel task thread.
			boolean hasCancelled = result.cancel(true);
			if (!hasCancelled) {
				LOGGER.warn("Task cannot be cancelled: " + key);
			}
		}
		if (task != null) {
			// Ensure task will cancel in loop.
			task.cancel();
		}
	}
	
	/**
	 *  Change key of task. 
	 */
	public void changeKey(K oldKey, K key) {
		synchronized (submissionMap) {
			if (!submissionMap.containsKey(oldKey)) {
				LOGGER.info("Task doesn't exist: " + oldKey);
				throw new IllegalArgumentException("oldKey");
			}
			Submission<V, S> submission = submissionMap.get(oldKey);
			if (submissionMap.containsKey(key)) {
				LOGGER.info("Task exists: " + key);
				throw new IllegalArgumentException("key");
			}
			LOGGER.debug("Change key from " + oldKey + " to " + key);
			submissionMap.remove(oldKey);
			submissionMap.put(key, submission);
		}
	}
	
	public void shutdown() {
		synchronized (submissionMap) {
			submissionMap.forEach((key, submission) -> {
				cancel(key, submission);
			});
		}
		executorService.shutdown();
	}
	
	public boolean isIdle() {
		boolean isIdle = true;
		synchronized (submissionMap) {
			if (submissionMap.size() > 0) {
				isIdle = false;
			}
		}		
		return isIdle;
	}
	
	public List<Report<K>> report() {
		List<Report<K>> reportMap = new ArrayList<>();
		List<K> removeList = new ArrayList<>();
		synchronized (submissionMap) {
			LOGGER.debug("Submission map size: " + submissionMap.size());
			submissionMap.forEach((key, submission) -> {
				Future<V> result = submission.getResult();
				Task<V, S> task = submission.getTask();
				Updatable<S> status = task.getStatus();
				Report<K> report = ReportFactory.report(key, task, result, status);

				// Withdraw report of submission that does not start.
				if (null != report) {
					reportMap.add(report);
					
					// Store key of submission that has done.
					if (report instanceof ResultReport) {
						LOGGER.debug("Remove done submission: " + key);
						removeList.add(key);
					}
				}
			});
			
			/* Remove submissions that has done. */
			if (removeList.size() > 0) {
				removeList.forEach(key -> submissionMap.remove(key));
			}
		}
		return reportMap;
	}
	
	private class Submission<T, R> {
		private final Task<T, R> task;
		private final Future<T> result;
		public Submission(Task<T, R> task, Future<T> result) {
			this.task = task;
			this.result = result;
		}
		public Task<T, R> getTask() {
			return task;
		}
		public Future<T> getResult() {
			return result;
		}
	}
}
