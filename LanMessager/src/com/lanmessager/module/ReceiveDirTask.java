package com.lanmessager.module;

import java.io.File;

import com.lanmessager.ui.ReceiveDirPanel;

public class ReceiveDirTask {
	
	private File dir;
	
	private ReceiveDirPanel panel;

	public File getDir() {
		return dir;
	}

	public void setDir(File dir) {
		this.dir = dir;
	}

	public ReceiveDirPanel getPanel() {
		return panel;
	}

	public void setPanel(ReceiveDirPanel panel) {
		this.panel = panel;
	}
	
}
