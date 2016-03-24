package com.lanmessager.ui;

public class SendFilePanel extends FileProcessPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String STATUS_LABEL_TEXT_START = "Sending file: %s";
	private static final String STATUS_LABEL_TEXT_SUCCEED = "Sent file successfully: %s";
	private static final String STATUS_LABEL_TEXT_FAIL = "Send file failed: %s";
	private static final String STATUS_LABEL_TEXT_CANCEL = "Send file canceled: %s";
	
	public SendFilePanel(String fileName) {
		super(fileName);
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
