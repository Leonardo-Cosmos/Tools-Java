package com.lanmessager.concurrent;

public class StatusReport<K, S> extends Report<K> {
	private S status;

	StatusReport(K key, S status) {
		super(key);
		this.status = status;
	}

	public S getStatus() {
		return status;
	}
}
