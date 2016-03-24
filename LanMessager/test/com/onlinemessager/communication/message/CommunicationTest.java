package com.onlinemessager.communication.message;

import org.junit.Test;

import com.lanmessager.communication.message.Message;
import com.lanmessager.communication.message.MessageJsonConverter;
import com.lanmessager.communication.message.SendFileMessage;

public class CommunicationTest {

	@Test
	public void test() {
		
	}

	@Test
	public void testChatMessageToJsonString() {
		SendFileMessage sendFileMessage = new SendFileMessage();
		sendFileMessage.setFileSize(0x400);
		sendFileMessage.setFileName("file.txt");
		
		Message sentMessage = sendFileMessage;
		String jsonString = MessageJsonConverter.toJson(sentMessage);
		System.out.println(jsonString);
		
		Message receivedMessage = MessageJsonConverter.parseJson(jsonString, SendFileMessage.class);
		sendFileMessage = (SendFileMessage) receivedMessage;
		System.out.println(sendFileMessage.getFileSize());
		System.out.println(sendFileMessage.getFileName());
	}
}
