package com.lanmessager.ui;

import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import com.lanmessager.file.FileDigestResult;
import com.lanmessager.file.FileLength;

public abstract class FileProcessPanel extends ProcessPanel {
	
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
	private static final String PROGRESS_LABEL_TEXT = "%s / %s (%s / s)";
	
	private static final String STATUS_LABEL_TEXT_INITIALIZE = "Ready to process file: %s";
	private static final String STATUS_LABEL_TEXT_ABORT = "Processing file is aborted: %s";
	private static final String STATUS_LABEL_TEXT_START = "Processing file: %s";
	private static final String STATUS_LABEL_TEXT_SUCCEED = "Processed file successfully: %s";
	private static final String STATUS_LABEL_TEXT_FAIL = "Process file failed: %s";
	private static final String STATUS_LABEL_TEXT_CANCEL = "Processing file is canceled: %s";
	
	protected final JLabel statusLabel;
	
	protected final JLabel progressLabel;
	
	protected final JProgressBar progressBar;
	
	protected final JButton cancelButton;
	
	protected final String fileName;
	
	protected long lastProcessed = 0;
	
	protected Date lastUpdateTime = null;
	
	public FileProcessPanel(String fileName) {
		this.fileName = fileName;
		
		BoxLayout boxLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(boxLayout);
		
		setBorder(BorderFactory.createEtchedBorder());
		
		statusLabel = new JLabel(String.format(getStatusLabelTextInitialize(), fileName));
		add(statusLabel);
		
		progressLabel = new JLabel();
		progressBar = new JProgressBar(PROGRESS_BAR_MIN, PROGRESS_BAR_MAX);
		cancelButton = new JButton(getCancelButtonText());
	}
	
	public void updateProgress(long processed, long total) {
		int percent = (int) (processed * PROGRESS_BAR_MAX / total);
		progressBar.setValue(percent);
		
		long updateProcessed = processed - lastProcessed;
		Date now = new Date();
		long updateTime = now.getTime() - lastUpdateTime.getTime();
		float updateSecond = updateTime / 1000f;
		long updatePerSecond = (long) (updateProcessed / updateSecond);
		
		progressLabel.setText(String.format(PROGRESS_LABEL_TEXT, 
				new FileLength(processed).toString(),
				new FileLength(total).toString(),
				new FileLength(updatePerSecond)));
		
		lastProcessed = processed;
		lastUpdateTime = now;
	}
	
	@Override
	public void succeed(Object result) {
		statusLabel.setText(String.format(getStatusLabelTextSucceed(), fileName));
		
		progressBar.setValue(PROGRESS_BAR_MAX);
		remove(progressLabel);
		
		remove(cancelButton);
		
		FileDigestResult fileDigestResult = (FileDigestResult) result;
		add(new JLabel(String.format(MD5_LABEL_TEXT, fileDigestResult.getMd5HexString())));
		add(new JLabel(String.format(SHA1_LABEL_TEXT, fileDigestResult.getSha1HexString())));
		add(new JLabel(String.format(SHA256_LABEL_TEXT, fileDigestResult.getSha256HexString())));
		
		validate();
		super.succeed(result);
	}
	
	@Override
	public void fail(Object cause) {
		statusLabel.setText(String.format(getStatusLabelTextFail(), fileName));
		
		remove(cancelButton);
		
		add(new JLabel((String) cause));
		
		validate();
		super.fail(cause);
	}
	
	@Override
	public void abort() {
		statusLabel.setText(String.format(getStatusLabelTextAbort(), fileName));
		
		validate();
		super.abort();
	}
	
	@Override
	public void start() {
		statusLabel.setText(String.format(getStatusLabelTextStart(), fileName));
		
		add(progressLabel);
		add(progressBar);		
		add(cancelButton);
		
		validate();
		super.start();
		
		// Add start time as first update time.
		lastUpdateTime = new Date();
	}
	
	@Override
	public void cancel() {
		statusLabel.setText(String.format(getStatusLabelTextCancel(), fileName));
		
		remove(cancelButton);
		
		validate();
		super.cancel();
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
