package com.lanmessager.module;

import java.io.File;
import java.util.Queue;

import com.lanmessager.ui.SendDirPanel;

public class SendDirTask {
	
	private File dir;
	
	private SendDirPanel panel;
	
	private String receiverAddress;

	private Queue<String> fileQueue;
	
	public File getDir() {
		return dir;
	}

	public void setDir(File dir) {
		this.dir = dir;
	}

	public SendDirPanel getPanel() {
		return panel;
	}

	public void setPanel(SendDirPanel panel) {
		this.panel = panel;
	}

	public String getReceiverAddress() {
		return receiverAddress;
	}

	public void setReceiverAddress(String receiverAddress) {
		this.receiverAddress = receiverAddress;
	}

	public Queue<String> getFileQueue() {
		return fileQueue;
	}

	public void setFileQueue(Queue<String> fileQueue) {
		this.fileQueue = fileQueue;
	}
	
}
