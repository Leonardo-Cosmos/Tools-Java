package com.lanmessager.backgroundworker.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;

public class Processor<K, V, S> {
	private static final Logger LOGGER = Logger.getLogger(Processor.class.getSimpleName());
	
	private final ExecutorService executor;
	
	private final Map<K, Submission<V, S>> submissionMap;
		
	public Processor(ExecutorService executor) {
		this.executor = executor;
		
		submissionMap = new HashMap<>();
	}
	
	public void submit(K key, Task<V, S> task) {
		task.setStatus(new FinalStatus<>());
		Future<V> result = executor.submit(task);
		synchronized (submissionMap) {
			if (submissionMap.containsKey(key)) {
				LOGGER.warn("Duplicated task: " + key);
			}			
			submissionMap.put(key, new Submission<>(task, result));
		}
	}
	
	public void cancel(K key) {
		Future<V> result = null;
		Task<V, S> task = null;
		synchronized (submissionMap) {
			if (!submissionMap.containsKey(key)) {
				LOGGER.warn("Task doesn't exist: " + key);
				return;
			}
			Submission<V, S> submission = submissionMap.get(key);
			result = submission.getResult();
			task = submission.getTask();
		}
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
	
	public void shutdown() {
		synchronized (submissionMap) {
			submissionMap.forEach((fileId, submission) -> {
				submission.getResult().cancel(true);
				submission.getTask().cancel();
			});
		}
		executor.shutdown();
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
						LOGGER.debug("Remove submission: " + key);
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
