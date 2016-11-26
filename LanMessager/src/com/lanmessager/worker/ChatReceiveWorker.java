package com.lanmessager.worker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import com.lanmessager.net.ChatServer;
import com.lanmessager.net.message.ReceiveFileMessage;
import com.lanmessager.net.message.SendFileMessage;
import com.lanmessager.net.packet.Packet;
import com.lanmessager.net.packet.PacketFactory;

public class ChatReceiveWorker {

	private static final Logger LOGGER = Logger.getLogger(ChatReceiveWorker.class.getSimpleName());
	
	private final ChatServer server;
	
	private final ChatServerSwingWorker worker;
	
	public ChatReceiveWorker() {
		server = new ChatServer();
		worker = new ChatServerSwingWorker();
	}
	
	public void start() {
		worker.execute();
	}
	
	public void stop() {
		server.stop();
	}
	
	protected void onHandlePacket(Packet packet) {
		switch (packet.getType()) {
		case PacketFactory.CHAT_TYPE_FILE_SEND:
			LOGGER.debug("On new send file message.");
			onSendFile((SendFileMessage) packet.getMessage()); 
			break;
		case PacketFactory.CHAT_TYPE_FILE_RECEIVE:
			LOGGER.debug("On new receive file message.");
			onReceiveFileAccept((ReceiveFileMessage) packet.getMessage());
		default:
			break;
		}
	}
	
	protected void onSendFile(SendFileMessage message) {
		if (sendFileListeners != null) {
			NewMessageEvent<SendFileMessage> event = new NewMessageEvent<>(this);
			event.setMessage(message);
			for (NewMessageListener<SendFileMessage> listener : sendFileListeners) {
				listener.handle(event);
			}
		}
	}
	
	protected void onReceiveFileAccept(ReceiveFileMessage message) {
		if (receiveFileListeners != null) {
			NewMessageEvent<ReceiveFileMessage> event = new NewMessageEvent<>(this);
			event.setMessage(message);
			for (NewMessageListener<ReceiveFileMessage> listener : receiveFileListeners) {
				listener.handle(event);
			}			
		}
	}
	
	private Set<NewMessageListener<SendFileMessage>> sendFileListeners;
	
	private Set<NewMessageListener<ReceiveFileMessage>> receiveFileListeners;

	public void addSendFileListener(NewMessageListener<SendFileMessage> listener) {
		if (sendFileListeners == null) {
			sendFileListeners = new HashSet<>();
		}
		sendFileListeners.add(listener);
	}
	
	public void removeSendFileListener(NewMessageListener<SendFileMessage> listener) {
		if (sendFileListeners != null) {
			sendFileListeners.remove(listener);
		}
	}

	public void addReceiveFileListener(NewMessageListener<ReceiveFileMessage> listener) {
		if (receiveFileListeners == null) {
			receiveFileListeners = new HashSet<>();
		}
		receiveFileListeners.add(listener);
	}

	public void removeReceiveFileListener(NewMessageListener<ReceiveFileMessage> listener) {
		if (receiveFileListeners != null) {
			receiveFileListeners.remove(listener);
		}
	}
	
	private class ChatServerSwingWorker extends SwingWorker<Void, Packet> {
		@Override
		protected Void doInBackground() throws Exception {
			server.addNewMessageListener(event -> publish(event.getPacket()));
			
			LOGGER.info("Chat server background thread starts.");
			server.start();
			
			LOGGER.info("Chat server background thread exits.");
			return null;
		}		
		@Override
		protected void done() {
			super.done();
		}		
		@Override
		protected void process(List<Packet> chunks) {
			super.process(chunks);
			
			for (Packet packet : chunks) {
				onHandlePacket(packet);
			}
		}
	}
}
