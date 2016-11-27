package com.lanmessager.ui;

import javax.swing.JLabel;

public class SendDirPanel extends DirProcessPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final String STATUS_LABEL_TEXT_INITIALIZE = "Ready to send directory: %s";
	private static final String STATUS_LABEL_TEXT_ABORT = "Sending directory is aborted: %s";
	private static final String STATUS_LABEL_TEXT_START = "Sending directory: %s";
	private static final String STATUS_LABEL_TEXT_SUCCEED = "Sent directory successfully: %s";
	private static final String STATUS_LABEL_TEXT_FAIL = "Send directory failed: %s";
	private static final String STATUS_LABEL_TEXT_CANCEL = "Sending directory is canceled: %s";
	
	private static final String RECEIVER_LABEL_TEXT = "To %s";
	
	public SendDirPanel(String dirName, String receiver) {
		super(dirName);
		
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
