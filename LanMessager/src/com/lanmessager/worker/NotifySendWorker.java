package com.lanmessager.worker;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import com.lanmessager.net.NotifyClient;
import com.lanmessager.net.message.Message;
import com.lanmessager.net.packet.Packet;

public class NotifySendWorker {

	private static final Logger LOGGER = Logger.getLogger(NotifySendWorker.class.getSimpleName());
	
	private final NotifyClient client;
	
	private final Queue<NotifyClientTask> taskQueue;
	
	private NotifyClientSwingWorker worker;
	
	public NotifySendWorker() {
		client = new NotifyClient();		
		taskQueue = new LinkedList<>();
	}
	
	/**
	 * Schedule a work thread to send message. 
	 * There is only one work thread, and messages will be processed in queue.
	 * 
	 * @param remoteHost
	 * @param message
	 */
	public void send(String remoteHost, Message message) {
		NotifyClientTask task = new NotifyClientTask();
		task.setRemoeteHost(remoteHost);
		task.setMessage(message);
		
		synchronized (task) {
			taskQueue.offer(task);
		}
		
		if (worker == null) {
			worker = new NotifyClientSwingWorker();
			worker.execute();			
		}
	}
	
	private class NotifyClientSwingWorker extends SwingWorker<Void, Packet> {
		@Override
		protected Void doInBackground() throws Exception {
			LOGGER.info("Notify client background thread starts.");
			
			while (true) {
				NotifyClientTask task = null;
				synchronized (taskQueue) {
					if (!taskQueue.isEmpty()) {
						task = taskQueue.poll();
					}
				}
				if (task != null) {
					try {
						client.send(task.getRemoeteHost(), task.getMessage());
					} catch (IOException ex) {
						LOGGER.error("Failed to send notify message.", ex);
					}
				} else {
					break;
				}
			}
			
			LOGGER.info("Notify client background thread exits.");
			return null;
		}
		@Override
		protected void done() {
			super.done();
			
			worker = null;
			
			boolean isQueueEmpty = false;
			synchronized (taskQueue) {
				isQueueEmpty = taskQueue.isEmpty();
			}
			
			if (!isQueueEmpty) {
				worker = new NotifyClientSwingWorker();
				worker.execute();
			}
		}
	}
	
	private class NotifyClientTask {
		private String remoeteHost;
		private Message message;
		public String getRemoeteHost() {
			return remoeteHost;
		}
		public void setRemoeteHost(String remoeteHost) {
			this.remoeteHost = remoeteHost;
		}
		public Message getMessage() {
			return message;
		}
		public void setMessage(Message message) {
			this.message = message;
		}		
	}
}
