package com.lanmessager.module;

import java.io.File;

import com.lanmessager.backgroundworker.FileReceiveWorker;
import com.lanmessager.ui.ReceiveFilePanel;

public class ReceiveFileTask {
	
	private File file;

	private long fileSize;
	
	private ReceiveFilePanel panel;
	
	private FileReceiveWorker receiver;

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

	public ReceiveFilePanel getPanel() {
		return panel;
	}

	public void setPanel(ReceiveFilePanel panel) {
		this.panel = panel;
	}

	public FileReceiveWorker getReceiver() {
		return receiver;
	}

	public void setReceiver(FileReceiveWorker receiver) {
		this.receiver = receiver;
	}
}
