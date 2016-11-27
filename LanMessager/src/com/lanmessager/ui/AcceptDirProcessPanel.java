package com.lanmessager.ui;

import java.awt.event.ActionListener;

import javax.swing.JButton;

public class AcceptDirProcessPanel extends DirProcessPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final String ACCEPT_BUTTON_TEXT = "Accept";
	private static final String ABORT_BUTTON_TEXT = "Abort";

	private final JButton acceptButton;

	private final JButton abortButton;

	public AcceptDirProcessPanel(String dirName) {
		super(dirName);
		
		acceptButton = new JButton(getAcceptButtonText());
		add(acceptButton);
		
		abortButton = new JButton(getAbortButtonText());
		add(abortButton);
	}
	
	@Override
	public void start() {
		remove(acceptButton);
		remove(abortButton);
		
		super.start();
	}
	
	@Override
	public void abort() {
		remove(acceptButton);
		remove(abortButton);
		
		super.abort();
	}
	
	protected String getAcceptButtonText() {
		return ACCEPT_BUTTON_TEXT;
	}
	
	protected String getAbortButtonText() {
		return ABORT_BUTTON_TEXT;
	}

	public void addAcceptButtonActionListener(ActionListener l) {
		acceptButton.addActionListener(l);
	}
	
	public void removeAcceptButtonActionListener(ActionListener l) {
		acceptButton.removeActionListener(l);
	}
	
	public void addAbortButtonActionListener(ActionListener l) {
		abortButton.addActionListener(l);
	}
	
	public void removeAbortButtonActionListener(ActionListener l) {
		abortButton.removeActionListener(l);
	}
}
