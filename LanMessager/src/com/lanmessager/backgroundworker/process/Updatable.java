package com.lanmessager.backgroundworker.process;

interface Updatable<S> {
	void update(S value);
	S get();
}
