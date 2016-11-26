package com.lanmessager.worker;

import java.util.EventListener;

import com.lanmessager.net.message.Message;

@FunctionalInterface
public interface NewMessageListener <T extends Message> extends EventListener {
	void handle(NewMessageEvent<T> e);
}
