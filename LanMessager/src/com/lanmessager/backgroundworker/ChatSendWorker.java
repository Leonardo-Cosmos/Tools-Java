package com.lanmessager.backgroundworker;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import com.lanmessager.communication.ChatClient;
import com.lanmessager.communication.message.Message;
import com.lanmessager.communication.packet.Packet;

public class ChatSendWorker {

	private static final Logger LOGGER = Logger.getLogger(ChatSendWorker.class.getSimpleName());
	
	private final ChatClient client;
	
	private final Queue<ChatClientTask> taskQueue;
	
	private ChatClientSwingWorker worker;
	
	public ChatSendWorker() {
		client = new ChatClient();		
		taskQueue = new LinkedList<>();
	}
	
	public void send(String remoteHost, Message message) {
		ChatClientTask task = new ChatClientTask();
		task.setRemoteHost(remoteHost);
		task.setMessage(message);
		
		synchronized (taskQueue) {
			taskQueue.offer(task);
		}
		
		if (worker == null) {
			worker = new ChatClientSwingWorker();
			worker.execute();
		}
	}
	
	private class ChatClientSwingWorker extends SwingWorker<Void, Packet> {
		@Override
		protected Void doInBackground() throws Exception {
			LOGGER.info("Chat client background thread starts.");
			
			while (true) {
				ChatClientTask task = null;
				synchronized (taskQueue) {
					if (!taskQueue.isEmpty()) {
						task = taskQueue.poll();
					}
				}
				if (task != null) {
					try {
						client.send(task.getRemoteHost(), task.getMessage());
					} catch (IOException ex) {
						LOGGER.error("Failed to send chat message.", ex);
					}
				} else {
					break;
				}
			}
			
			LOGGER.info("Chat client background thread exits.");
			return null;
		};
		
		@Override
		protected void done() {
			super.done();	
			
			worker = null;
			
			boolean isQueueEmpty = false;
			synchronized (taskQueue) {
				isQueueEmpty = taskQueue.isEmpty();
			}
			
			if (!isQueueEmpty) {
				worker = new ChatClientSwingWorker();
				worker.execute();
			}
		}
	}
	
	private class ChatClientTask {
		private String remoteHost;
		private Message message;
		public String getRemoteHost() {
			return remoteHost;
		}
		public void setRemoteHost(String remoteHost) {
			this.remoteHost = remoteHost;
		}
		public Message getMessage() {
			return message;
		}
		public void setMessage(Message message) {
			this.message = message;
		}		
	}
}
