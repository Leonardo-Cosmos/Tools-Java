package com.lanmessager.backgroundworker.process;

import java.util.concurrent.Callable;

public abstract class Task<V> implements Callable<V> {
	
	private boolean isStarted = false;
	
	private boolean isDone = false;
	
	private boolean isCancelled = false;

	public boolean isStarted() {
		return isStarted;
	}

	public boolean isCancelled() {
		return isCancelled;
	}
	
	public boolean isDone() {
		return isDone;
	}
	
	public void cancel() {
		isCancelled = true;
	}
	
	protected void start() {
		isStarted = true;
	}
	
	protected void done() {
		isDone = true;
	}
}
