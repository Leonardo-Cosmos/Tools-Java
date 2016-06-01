package com.lanmessager.ui;

import javax.swing.JLabel;

public class ReceiveFilePanel extends AcceptFileProcessPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String STATUS_LABEL_TEXT_INITIALIZE = "Ready to receive file: %s";
	private static final String STATUS_LABEL_TEXT_ABORT = "Receiving file is aborted: %s";
	private static final String STATUS_LABEL_TEXT_START = "Receiving file: %s";
	private static final String STATUS_LABEL_TEXT_SUCCEED = "Received file successfully: %s";
	private static final String STATUS_LABEL_TEXT_FAIL = "Receive file failed: %s";
	private static final String STATUS_LABEL_TEXT_CANCEL = "Receiving file canceled: %s";
	
	private static final String SEND_LABEL_TEXT = "From %s";
	
	public ReceiveFilePanel(String fileName, String sender) {
		super(fileName);
		
		add(new JLabel(String.format(SEND_LABEL_TEXT, sender)), 0);
	}

	@Override
	protected String getStatusLabelTextInitialize() {
		return STATUS_LABEL_TEXT_INITIALIZE;
	}
	
	@Override
	protected String getStatusLabelTextAbort() {
		return STATUS_LABEL_TEXT_ABORT;
	}
	
	@Override
	protected String getStatusLabelTextStart() {
		return STATUS_LABEL_TEXT_START;
	}

	@Override
	protected String getStatusLabelTextSucceed() {
		return STATUS_LABEL_TEXT_SUCCEED;
	}

	@Override
	protected String getStatusLabelTextFail() {
		return STATUS_LABEL_TEXT_FAIL;
	}
	
	@Override
	protected String getStatusLabelTextCancel() {
		return STATUS_LABEL_TEXT_CANCEL;
	}
}
