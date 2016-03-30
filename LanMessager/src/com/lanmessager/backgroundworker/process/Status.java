package com.lanmessager.backgroundworker.process;

public interface Status<S> {
	void update(S value);
	S get();
}
