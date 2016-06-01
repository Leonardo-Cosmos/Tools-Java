package com.lanmessager.communication;

import org.junit.Test;

import com.lanmessager.communication.NotifyServer;

public class NotifyServerTest extends BaseServerTest {
	//private static final Logger LOGGER = Logger.getLogger(NotifyServerTest.class.getSimpleName());

	@Test
	public void test() {
		NotifyServer server = new NotifyServer();
		/*
		server.addFriendOnlineListener((message) -> {
			System.out.println("Friend online:");
			System.out.println(message.getName());
			System.out.println(message.getAddress());
		});
		server.addFriendOfflineListener((message) -> {
			System.out.println("Friend offline:");
			System.out.println(message.getName());
			System.out.println(message.getAddress());
		});
		*/
		server.start();

		holdServer();
	}
}
