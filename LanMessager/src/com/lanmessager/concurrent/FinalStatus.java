package com.lanmessager.concurrent;

/**
 * Saves final status of {@link Task}.
 *
 * @param <S> The status type updated by {@link Task}.
 */
class FinalStatus<S> implements Updatable<S> {

	private volatile S value;
	
	@Override
	public void update(S value) {
		this.value = value;
	}

	@Override
	public S get() {
		return value;
	}
	
}
