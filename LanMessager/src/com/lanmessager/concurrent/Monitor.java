package com.lanmessager.concurrent;

public abstract class Monitor<K, V, S> {
	
	abstract void wakeup();
	
	protected abstract void onDone(ResultReport<K, V> report);
	
	protected abstract void onStatusUpdated(StatusReport<K, S> report);
}
