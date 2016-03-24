package com.lanmessager.communication.packet;

import java.util.HashMap;
import java.util.Map;

import com.lanmessager.communication.message.FriendOfflineMessage;
import com.lanmessager.communication.message.FriendOnlineMessage;
import com.lanmessager.communication.message.Message;
import com.lanmessager.communication.message.MessageJsonConverter;
import com.lanmessager.communication.message.ReceiveFileMessage;
import com.lanmessager.communication.message.SendFileMessage;

public class PacketFactory {

	public static final int NOTIFY_TYPE_FRIEND_ONLINE = 0x001;
	public static final int NOTIFY_TYPE_FRIEND_OFFLINE = 0x002;

	public static final int CHAT_TYPE_FILE_SEND = 0x101;
	public static final int CHAT_TYPE_FILE_SEND_CANCEL = 0x102;
	public static final int CHAT_TYPE_FILE_RECEIVE = 0x111;
	//public static final int CHAT_TYPE_FILE_RECEIVE_REJECT = 0x012;
	
	private static final int TYPE_LENGTH = 8;
	private static final int TYPE_RADIX = 16;
	private static final String TYPE_FORMAT = "%08x";

	static Map<Integer, Class<? extends Message>> messageClassMap; 

	static {
		messageClassMap = new HashMap<>();
		messageClassMap.put(CHAT_TYPE_FILE_SEND, SendFileMessage.class);
		messageClassMap.put(CHAT_TYPE_FILE_RECEIVE, ReceiveFileMessage.class);
		//messageClassMap.put(CHAT_TYPE_FILE_RECEIVE_REJECT, ReceiveFileRejectMessage.class);
		messageClassMap.put(NOTIFY_TYPE_FRIEND_ONLINE, FriendOnlineMessage.class);
		messageClassMap.put(NOTIFY_TYPE_FRIEND_OFFLINE, FriendOfflineMessage.class);
	}
	
	public static Packet createPacket(Message message) {
		int type = 0;
		if (message instanceof SendFileMessage) {
			type = CHAT_TYPE_FILE_SEND;
		} else if (message instanceof ReceiveFileMessage) {
			type = CHAT_TYPE_FILE_RECEIVE;
		} else if (message instanceof FriendOnlineMessage) {
			type = NOTIFY_TYPE_FRIEND_ONLINE;
		} else if (message instanceof FriendOfflineMessage) {
			type = NOTIFY_TYPE_FRIEND_OFFLINE;
		}
		Packet packet = new Packet();
		packet.setType(type);
		packet.setMessage(message);
		return packet;
	}
	
	public static String toString(Packet packet) {
		String typeString = String.format(TYPE_FORMAT, packet.getType());
		String messageString = MessageJsonConverter.toJson(packet.getMessage());
		return typeString + messageString;
	}

	public static Packet parse(String str) {
		String typeString = str.substring(0, TYPE_LENGTH);
		String messageString = str.substring(TYPE_LENGTH);
		Packet packet = new Packet();
		int type = Integer.parseInt(typeString, TYPE_RADIX);
		packet.setType(type);
		packet.setMessage(MessageJsonConverter.parseJson(messageString, getClass(type)));
		return packet;
	}

	private static Class<? extends Message> getClass(int type) {
		Class<? extends Message> clazz = messageClassMap.get(type);
		if (clazz == null) {
			clazz = Message.class;
		}
		return clazz;
	}
}
