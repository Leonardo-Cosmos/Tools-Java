package com.lanmessager.backgroundworker;

import java.util.EventListener;

import com.lanmessager.communication.message.Message;

@FunctionalInterface
public interface NewMessageListener <T extends Message> extends EventListener {
	void handle(NewMessageEvent<T> e);
}
