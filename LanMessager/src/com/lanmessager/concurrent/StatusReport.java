package com.lanmessager.concurrent;

/**
 * Represents information during {@link Task} is executing.
 *
 * @param <K> The key type used to retrieve {@link Task}.
 * @param <S> The status type updated by {@link Task}.
 */
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
