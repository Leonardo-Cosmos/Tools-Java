package com.lanmessager.module;

import java.io.File;

import com.lanmessager.ui.SendDirPanel;

public class SendDirTask {
	
	private File dir;
	
	private SendDirPanel panel;

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
	
}
