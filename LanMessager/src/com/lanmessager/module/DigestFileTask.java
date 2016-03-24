package com.lanmessager.module;

import java.io.File;

import com.lanmessager.ui.DigestFilePanel;

public class DigestFileTask {

	private File file;
	
	private DigestFilePanel panel;

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public DigestFilePanel getPanel() {
		return panel;
	}

	public void setPanel(DigestFilePanel panel) {
		this.panel = panel;
	}
}
