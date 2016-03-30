package com.lanmessager.backgroundworker.process;

public class ResultReport<K, V> extends Report<K> {

	private V result;
	
	private Exception cause;
	
	private boolean isDone = false;
	
	private boolean isCancelled = false;
	
	ResultReport(K key) {
		super(key);
	}
	
	public V getResult() {
		return result;
	}

	void setResult(V result) {
		this.result = result;
	}

	public Exception getCause() {
		return cause;
	}

	void setCause(Exception failureCause) {
		this.cause = failureCause;
	}

	public boolean isDone() {
		return isDone;
	}

	void setDone(boolean isDone) {
		this.isDone = isDone;
	}

	public boolean isCancelled() {
		return isCancelled;
	}

	void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}
}
