package com.lanmessager.ui;

import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.lanmessager.file.FileDigestResult;

public abstract class FileProcessPanel extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final String MD5_LABEL_TEXT = "MD5: %s";
	private static final String SHA1_LABEL_TEXT = "SHA1: %s";
	private static final String SHA256_LABEL_TEXT = "SHA256: %s";
	
	private static final String CANCEL_BUTTON_TEXT = "Cancel";
	
	private static final int PROGRESS_BAR_MIN = 0;
	private static final int PROGRESS_BAR_MAX = 100;
	
	protected final JLabel statusLabel;
	
	protected final JProgressBar progressBar;
	
	protected final JButton cancelButton;
	
	protected final String fileName;
	
	public FileProcessPanel(String fileName) {
		this.fileName = fileName;
		
		BoxLayout boxLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(boxLayout);
		
		statusLabel = new JLabel(String.format(getStatusLabelTextStart(), fileName));
		add(statusLabel);
		
		progressBar = new JProgressBar(PROGRESS_BAR_MIN, PROGRESS_BAR_MAX);	
		add(progressBar);
		
		cancelButton = new JButton(getCancelButtonText());
		add(cancelButton);
	}
	
	public void updateProgress(long processed, long total) {
		int percent = (int) (processed * PROGRESS_BAR_MAX / total);
		progressBar.setValue(percent);
	}
	
	public void complete(FileDigestResult result) {
		progressBar.setValue(PROGRESS_BAR_MAX);
		
		statusLabel.setText(String.format(getStatusLabelTextSucceed(), fileName));
		
		add(new JLabel(String.format(MD5_LABEL_TEXT, result.getMd5HexString())));
		add(new JLabel(String.format(SHA1_LABEL_TEXT, result.getSha1HexString())));
		add(new JLabel(String.format(SHA256_LABEL_TEXT, result.getSha256HexString())));
		validate();
	}
	
	public void fail(String cause) {
		statusLabel.setText(String.format(getStatusLabelTextFail(), fileName));
		
		add(new JLabel(cause));
		validate();
	}
	
	public void cancel() {
		statusLabel.setText(String.format(getStatusLabelTextCancel(), fileName));
		remove(cancelButton);
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
	
	protected abstract String getStatusLabelTextStart();
	protected abstract String getStatusLabelTextSucceed();
	protected abstract String getStatusLabelTextFail();
	protected abstract String getStatusLabelTextCancel();
}
