package com.lanmessager.ui;

import java.awt.Component;

import javax.swing.JPanel;

public class ChatMessagePanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	Component verticalStrut;

	Component getVerticalStrut() {
		return verticalStrut;
	}

	void setVerticalStrut(Component verticalStrut) {
		this.verticalStrut = verticalStrut;
	}
	
}
