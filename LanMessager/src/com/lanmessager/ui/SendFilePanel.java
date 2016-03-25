package com.lanmessager.ui;

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
	
	public SendFilePanel(String fileName) {
		super(fileName);
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
