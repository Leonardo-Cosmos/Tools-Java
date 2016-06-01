package com.lanmessager.communication;

import java.io.IOException;

import org.junit.Test;

import com.lanmessager.communication.NotifyClient;
import com.lanmessager.communication.message.FriendOfflineMessage;
import com.lanmessager.communication.message.FriendOnlineMessage;

public class NotifyClientTest extends BaseClientTest {
	//private static final Logger LOGGER = Logger.getLogger(NotifyClientTest.class.getSimpleName());

	@Test
	public void testFriendOnline() {
		NotifyClient client = new NotifyClient();		
		FriendOnlineMessage message = new FriendOnlineMessage();
		message.setName("Test");
		message.setAddress("172.19.3.140");
		try {
			client.send("localhost", message);
		} catch (IOException ex) {
			printException("Error when communicate with remote host.", ex);
		}
	}

	@Test
	public void testFriendOffline() {
		NotifyClient client = new NotifyClient();		
		FriendOfflineMessage message = new FriendOfflineMessage();
		message.setName("Test");
		message.setAddress("172.19.3.140");
		try {
			client.send("localhost", message);
		} catch (IOException ex) {
			printException("Error when communicate with remote host.", ex);
		}
	}
}
