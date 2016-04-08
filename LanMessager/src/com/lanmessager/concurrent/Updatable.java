package com.lanmessager.concurrent;

interface Updatable<S> {
	void update(S value);
	S get();
}
