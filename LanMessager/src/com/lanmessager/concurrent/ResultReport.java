package com.lanmessager.concurrent;

public class ResultReport<K, V> extends Report<K> {

	private V result;
	
	private Exception cause;
	
	private boolean isCancelled = false;
	
	ResultReport(K key) {
		super(key);
	}
	
	public V getResult() throws Exception {
		if (null != cause) {
			throw cause;
		}
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

	public boolean isCancelled() {
		return isCancelled;
	}

	void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}
}
