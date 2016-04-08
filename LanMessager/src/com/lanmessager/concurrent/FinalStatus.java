package com.lanmessager.concurrent;

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
