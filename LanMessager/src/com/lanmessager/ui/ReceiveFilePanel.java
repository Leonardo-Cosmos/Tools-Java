package com.lanmessager.ui;

public class ReceiveFilePanel extends FileProcessPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String STATUS_LABEL_TEXT_START = "Receiving file: %s";
	private static final String STATUS_LABEL_TEXT_SUCCEED = "Received file successfully: %s";
	private static final String STATUS_LABEL_TEXT_FAIL = "Receive file failed: %s";
	private static final String STATUS_LABEL_TEXT_CANCEL = "Receive file canceled: %s";
	
	public ReceiveFilePanel(String fileName) {
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
