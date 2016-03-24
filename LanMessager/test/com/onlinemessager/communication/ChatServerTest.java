package com.onlinemessager.communication;

import org.junit.Test;

import com.lanmessager.communication.ChatServer;

public class ChatServerTest extends BaseServerTest {
	//private static final Logger LOGGER = Logger.getLogger(ChatServerTest.class.getSimpleName());
	
	@Test
	public void test() {
		ChatServer server = new ChatServer();
		/*
		server.addSendFileListener(message -> {
			System.out.println("Friend send file to me");
			System.out.println(message.getFileId());
			System.out.println(message.getFileName());
			System.out.println(message.getFileSize());
			System.out.println(message.getSenderAddress());
		});
		server.addReceiveFileListener(message -> {
			System.out.println("Friend send file to me");
			System.out.println(message.getFileId());
			System.out.println(message.getReceiverPort());
		});
		*/
		server.start();
		
		holdServer();
	}
}
