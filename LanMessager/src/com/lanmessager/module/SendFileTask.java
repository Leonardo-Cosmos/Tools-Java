package com.lanmessager.module;

import java.io.File;

import com.lanmessager.backgroundworker.FileSendWorker;
import com.lanmessager.ui.SendFilePanel;

public class SendFileTask {

	private File file;

	private long fileSize;
	
	private String receiverAddress;
	
	private SendFilePanel panel;
	
	private FileSendWorker sender;

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public String getAddress() {
		return receiverAddress;
	}

	public void setAddress(String address) {
		this.receiverAddress = address;
	}

	public SendFilePanel getPanel() {
		return panel;
	}

	public void setPanel(SendFilePanel panel) {
		this.panel = panel;
	}

	public FileSendWorker getSender() {
		return sender;
	}

	public void setSender(FileSendWorker sender) {
		this.sender = sender;
	}
}
