package com.lanmessager.communication;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.lanmessager.communication.ChatClient;
import com.lanmessager.communication.message.SendFileMessage;

public class ChatClientTest extends BaseClientTest {
	private static final Logger LOGGER = Logger.getLogger(ChatClientTest.class.getSimpleName());
	
	@Test
	public void testSendFile() {
		ChatClient client = new ChatClient();
		SendFileMessage message = new SendFileMessage();
		message.setFileId("lkwjoijd");
		message.setFileName("test.txt");
		message.setFileSize(0x400);
		message.setSenderAddress("10.27.212.82");
		try {
			client.send("localhost", message);
		} catch (IOException ex) {
			LOGGER.error("Error when communicate with server.", ex);
		}
	}

}
