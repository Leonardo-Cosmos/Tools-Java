package com.lanmessager.ui;

public class ReceiveFilePanel extends FileProcessPanel {

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
	
	// XXX Support receive button in panel.
	public ReceiveFilePanel(String fileName) {
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
