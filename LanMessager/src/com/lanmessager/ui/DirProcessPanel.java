package com.lanmessager.ui;

import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public abstract class DirProcessPanel extends ChatMessagePanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String STATUS_INITIALIZE = "Initialize";
	public static final String STATUS_ABORT = "Abort";
	public static final String STATUS_START = "Start";
	public static final String STATUS_SUCCEED = "Succeed";
	public static final String STATUS_FAIL = "Fail";
	public static final String STATUS_CANCEL = "Cancel";

	private static final String CANCEL_BUTTON_TEXT = "Cancel";

	private static final String STATUS_LABEL_TEXT_INITIALIZE = "Ready to process directory: %s";
	private static final String STATUS_LABEL_TEXT_ABORT = "Processing directory is aborted: %s";
	private static final String STATUS_LABEL_TEXT_START = "Processing directory: %s";
	private static final String STATUS_LABEL_TEXT_SUCCEED = "Processed directory successfully: %s";
	private static final String STATUS_LABEL_TEXT_FAIL = "Process directory failed: %s";
	private static final String STATUS_LABEL_TEXT_CANCEL = "Processing directory is canceled: %s";

	protected final JLabel statusLabel;
	
	protected final JPanel fileProcessPanel;

	protected final JButton cancelButton;

	protected final String dirName;

	private String status;

	public DirProcessPanel(String dirName) {
		this.dirName = dirName;

		BoxLayout boxLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(boxLayout);

		setBorder(BorderFactory.createEtchedBorder());

		statusLabel = new JLabel(String.format(getStatusLabelTextInitialize(), dirName));
		add(statusLabel);

		fileProcessPanel = new JPanel();
		BoxLayout panelBoxLayout = new BoxLayout(fileProcessPanel, BoxLayout.Y_AXIS);
		fileProcessPanel.setLayout(panelBoxLayout);
		
		cancelButton = new JButton(getCancelButtonText());

		status = STATUS_INITIALIZE;
	}

	public void succeed() {
		statusLabel.setText(String.format(getStatusLabelTextSucceed(), dirName));

		remove(cancelButton);

		validate();
		status = STATUS_SUCCEED;
	}

	public void fail(String cause) {
		statusLabel.setText(String.format(getStatusLabelTextFail(), dirName));

		remove(cancelButton);

		add(new JLabel(cause));

		validate();
		status = STATUS_FAIL;
	}
	
	public void abort() {
		statusLabel.setText(String.format(getStatusLabelTextAbort(), dirName));
		
		validate();
		status = STATUS_ABORT;
	}
	
	public void start() {
		statusLabel.setText(String.format(getStatusLabelTextStart(), dirName));
		
		add(fileProcessPanel);
		add(cancelButton);
		
		validate();
		status = STATUS_START;
	}
	
	public void cancel() {
		statusLabel.setText(String.format(getStatusLabelTextCancel(), dirName));
		
		remove(cancelButton);
		
		validate();
		status = STATUS_CANCEL;
	}

	public void addCancelButtonActionListener(ActionListener l) {
		cancelButton.addActionListener(l);
	}

	public void removeCancelButtonActionListener(ActionListener l) {
		cancelButton.removeActionListener(l);
	}

	public String getStatus() {
		return status;
	}

	protected String getCancelButtonText() {
		return CANCEL_BUTTON_TEXT;
	}

	protected String getStatusLabelTextInitialize() {
		return STATUS_LABEL_TEXT_INITIALIZE;
	}

	protected String getStatusLabelTextAbort() {
		return STATUS_LABEL_TEXT_ABORT;
	}

	protected String getStatusLabelTextStart() {
		return STATUS_LABEL_TEXT_START;
	}

	protected String getStatusLabelTextSucceed() {
		return STATUS_LABEL_TEXT_SUCCEED;
	}

	protected String getStatusLabelTextFail() {
		return STATUS_LABEL_TEXT_FAIL;
	}

	protected String getStatusLabelTextCancel() {
		return STATUS_LABEL_TEXT_CANCEL;
	}

}
