package com.lanmessager.ui;

import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public abstract class DirProcessPanel extends ProcessPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

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
	}

	@Override
	public void succeed(Object result) {
		statusLabel.setText(String.format(getStatusLabelTextSucceed(), dirName));

		remove(cancelButton);

		validate();
		super.succeed(result);
	}

	@Override
	public void fail(Object cause) {
		statusLabel.setText(String.format(getStatusLabelTextFail(), dirName));

		remove(cancelButton);

		add(new JLabel((String) cause));

		validate();
		super.fail(cause);
	}
	
	@Override
	public void abort() {
		statusLabel.setText(String.format(getStatusLabelTextAbort(), dirName));
		
		validate();
		super.abort();
	}
	
	@Override
	public void start() {
		statusLabel.setText(String.format(getStatusLabelTextStart(), dirName));
		
		add(fileProcessPanel);
		add(cancelButton);
		
		validate();
		super.start();
	}
	
	@Override
	public void cancel() {
		statusLabel.setText(String.format(getStatusLabelTextCancel(), dirName));
		
		remove(cancelButton);
		
		validate();
		super.cancel();
	}
	
	public void addPanel(FileProcessPanel panel) {
		if (panel == null) {
			return;
		}
		
		add(panel);
		updateUI();
	}

	public JPanel getFileProcessPanel() {
		return fileProcessPanel;
	}

	public void addCancelButtonActionListener(ActionListener l) {
		cancelButton.addActionListener(l);
	}

	public void removeCancelButtonActionListener(ActionListener l) {
		cancelButton.removeActionListener(l);
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
