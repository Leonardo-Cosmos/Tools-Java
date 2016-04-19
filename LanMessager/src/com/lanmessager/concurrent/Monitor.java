package com.lanmessager.concurrent;

/**
 * A abstract class to perform monitoring status and task result of {@link TaskExecutor}.
 *
 * @param <K> The key type used to retrieve {@link Task}.
 * @param <V> The result type returned by {@link Task}.
 * @param <S> The status type updated by {@link Task}.
 */
public abstract class Monitor<K, V, S> {
	
	abstract void wakeup();
	
	protected abstract void onDone(ResultReport<K, V> report);
	
	protected abstract void onStatusUpdated(StatusReport<K, S> report);
}
