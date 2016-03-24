package com.lanmessager.communication.message;

@Deprecated
@FunctionalInterface
public interface NotifyListener {
	void handle(int type, String name, String address);
}
