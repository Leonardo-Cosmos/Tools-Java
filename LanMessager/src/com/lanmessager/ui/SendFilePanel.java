package com.lanmessager.ui;

import javax.swing.JLabel;

public class SendFilePanel extends FileProcessPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String STATUS_LABEL_TEXT_INITIALIZE = "Ready to send file: %s";
	private static final String STATUS_LABEL_TEXT_ABORT = "Sending file is aborted: %s";
	private static final String STATUS_LABEL_TEXT_START = "Sending file: %s";
	private static final String STATUS_LABEL_TEXT_SUCCEED = "Sent file successfully: %s";
	private static final String STATUS_LABEL_TEXT_FAIL = "Send file failed: %s";
	private static final String STATUS_LABEL_TEXT_CANCEL = "Sending file is canceled: %s";
	
	private static final String RECEIVER_LABEL_TEXT = "To %s";
	
	public SendFilePanel(String fileName, String receiver) {
		super(fileName);
		
		add(new JLabel(String.format(RECEIVER_LABEL_TEXT, receiver)), 0);
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
