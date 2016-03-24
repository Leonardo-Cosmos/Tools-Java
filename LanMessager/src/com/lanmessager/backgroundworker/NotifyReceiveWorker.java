package com.lanmessager.backgroundworker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import com.lanmessager.communication.NotifyServer;
import com.lanmessager.communication.message.FriendOfflineMessage;
import com.lanmessager.communication.message.FriendOnlineMessage;
import com.lanmessager.communication.packet.Packet;
import com.lanmessager.communication.packet.PacketFactory;

public class NotifyReceiveWorker {

	private static final Logger LOGGER = Logger.getLogger(NotifyReceiveWorker.class.getSimpleName());

	private final NotifyServer server;

	private final NotifyServerSwingWorker worker;

	public NotifyReceiveWorker() {
		server = new NotifyServer();
		worker = new NotifyServerSwingWorker();
	}

	public void start() {
		worker.execute();
	}
	
	public void stop() {
		server.stop();
	}

	protected void onHandlePacket(Packet packet) {
		switch (packet.getType()) {
		case PacketFactory.NOTIFY_TYPE_FRIEND_ONLINE:
			LOGGER.debug("On new friend online message.");
			onFriendOnline((FriendOnlineMessage) packet.getMessage());
			break;
		case PacketFactory.NOTIFY_TYPE_FRIEND_OFFLINE:
			LOGGER.debug("On new friend offline message.");
			onFriendOffline((FriendOfflineMessage) packet.getMessage());
			break;
		default:
			break;
		}
	}

	protected void onFriendOnline(FriendOnlineMessage message) {
		if (friendOnlineListeners != null) {
			NewMessageEvent<FriendOnlineMessage> event = new NewMessageEvent<>(this);
			event.setMessage(message);
			for (NewMessageListener<FriendOnlineMessage> listener : friendOnlineListeners) {
				listener.handle(event);
			}
		}
	}

	protected void onFriendOffline(FriendOfflineMessage message) {
		if (friendOfflineListeners != null) {
			NewMessageEvent<FriendOfflineMessage> event = new NewMessageEvent<>(this);
			event.setMessage(message);
			for (NewMessageListener<FriendOfflineMessage> listener : friendOfflineListeners) {
				listener.handle(event);
			}
		}
	}

	private Set<NewMessageListener<FriendOnlineMessage>> friendOnlineListeners;

	private Set<NewMessageListener<FriendOfflineMessage>> friendOfflineListeners;

	public void addFriendOnlineListener(NewMessageListener<FriendOnlineMessage> listener) {
		if (friendOnlineListeners == null) {
			friendOnlineListeners = new HashSet<>();
		}		
		friendOnlineListeners.add(listener);
	}
	
	public void removeFriendOnlineListener(NewMessageListener<FriendOnlineMessage> listener) {
		if (friendOnlineListeners != null) {
			friendOnlineListeners.remove(listener);
		}
	}

	public void addFriendOfflineListener(NewMessageListener<FriendOfflineMessage> listener) {
		if (friendOfflineListeners == null) {
			friendOfflineListeners = new HashSet<>();
		}
		friendOfflineListeners.add(listener);
	}
	
	public void removeFriendOfflineListener(NewMessageListener<FriendOfflineMessage> listener) {
		if (friendOfflineListeners != null) {
			friendOfflineListeners.remove(listener);
		}
	}

	private class NotifyServerSwingWorker extends SwingWorker<Void, Packet> {
		@Override
		protected Void doInBackground() throws Exception {
			server.addNewMessageListener(event -> publish(event.getPacket()));
			
			LOGGER.info("Notify server background thread starts.");
			server.start();

			LOGGER.info("Notify server background thread exits.");
			return null;
		}
		@Override
		protected void done() {
			super.done();
		}
		@Override
		protected void process(List<Packet> chunks) {
			super.process(chunks);

			Packet packet = chunks.get(0);
			onHandlePacket(packet);
		}
	}
}
