package com.lanmessager.concurrent;

import java.util.concurrent.Callable;

public abstract class Task<V, S> implements Callable<V> {
	
	private volatile boolean isStarted = false;
	
	private volatile boolean isDone = false;
	
	private volatile boolean isCancelled = false;

	private Updatable<S> status;

	public boolean isStarted() {
		return isStarted;
	}

	public boolean isCancelled() {
		return isCancelled;
	}
	
	public boolean isDone() {
		return isDone;
	}
	
	public Updatable<S> getStatus() {
		return status;
	}

	public void setStatus(Updatable<S> status) {
		this.status = status;
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
	
	@Override
	public final V call() throws Exception {
		start();
		try {
			return execute();
		} finally {
			done();
		}
	}
	
	protected abstract V execute() throws Exception;
	
	protected void onStatusUpdated(S status) {
		if (null != this.status) {
			this.status.update(status);
		}
	}
}
