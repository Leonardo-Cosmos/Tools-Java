package com.lanmessager.backgroundworker.process;

import java.util.concurrent.Future;

public class ReportFactory {
	
	public static<K, V, S> Report<K> report(K key, Task<V> task, Future<V> result, Status<S> status) {
		/*
		 * Task ready to start. 
		 * 		Result not done (not cancelled). Task not done, not cancelled. Status not existed.
		 * Task is cancelled before start and done. 
		 * 		Result cancelled (done). Task not done, cancelled. Status not existed.
		 * Task is cancelled after start and still running.
		 * 		Result cancelled (done). Task not done, cancelled. Status existed.
		 * Task is cancelled after start and done.
		 * 		Result cancelled (done). Task done, cancelled. Status existed.
		 * Task is running without cancellation.
		 * 		Result not done (not cancelled). Task not done, not cancelled. Status existed.
		 * Task is done without cancellation.
		 * 		Result done, not cancelled. Task done, not cancelled. Status existed.
		 * 
		 * Result is always done when it is cancelled.
		 * Task maybe not done when it is cancelled.
		 */
		Report<K> report;
		if (!result.isDone() && !task.isDone() && !task.isCancelled() && task.isStarted()) {
			// Task ready to start.
			report = null;
			
		} else if (result.isDone() && !result.isCancelled() && task.isDone() && !task.isCancelled() && task.isStarted()) {
			// Task is done without cancellation.
			report = reportResult(key, true, false, result);
			
		} else if (!result.isDone() && !task.isDone() && !task.isCancelled() && task.isStarted()) {
			// Task is running without cancellation.
			report = reportStatus(key, status);
			
		} else if (result.isCancelled() && !task.isDone() && task.isCancelled() && !task.isStarted()) {
			// Task is cancelled before start and done.
			report = reportResult(key, true, true, result);
			
		} else if (result.isCancelled() && !task.isDone() && task.isCancelled() && task.isStarted()) {
			// Task is cancelled after start and still running.
			report = reportStatus(key, status);
			
		} else if (result.isCancelled() && task.isDone() && task.isCancelled() && task.isStarted()) {
			// Task is cancelled after start and done.
			report = reportResult(key, true, true, result);
			
		} else {
			report = null;
		}
		
		return report;
	}
	
	private static<K, V> ResultReport<K, V> reportResult(K key, boolean isDone, boolean isCancelled, Future<V> result) {
		ResultReport<K, V> resultReport = new ResultReport<>(key);
		resultReport.setDone(isDone);
		resultReport.setCancelled(isCancelled);
		if (isDone && !isCancelled) {
			try {
				resultReport.setResult(result.get());
			} catch (Exception ex) {
				resultReport.setCause(ex);
			}
		}
		return resultReport;
	}
	
	private static<K, S> StatusReport<K, S> reportStatus(K key, Status<S> status) {
		if (null != status.get()) {
			StatusReport<K, S> statusReport = new StatusReport<K, S>(key, status.get());
			return statusReport;
		} else {
			return null;
		}
	}
}
