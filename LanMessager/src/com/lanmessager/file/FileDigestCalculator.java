package com.lanmessager.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import com.lanmessager.file.ProgressUpdatedEvent;
import com.lanmessager.file.ProgressUpdatedListener;

public class FileDigestCalculator {
	private static final int BUFFER_LENGTH = 0x1000;
	
	private volatile boolean isCanceled = false;
	
	private Set<ProgressUpdatedListener> progressUpdatedListeners;

	public void addProgressUpdatedListener(ProgressUpdatedListener listener) {
		if (progressUpdatedListeners == null) {
			progressUpdatedListeners = new HashSet<>();
		}
		progressUpdatedListeners.add(listener);
	}
	
	public void removeProgressUpdatedListener(ProgressUpdatedListener listener) {
		if (progressUpdatedListeners != null) {
			progressUpdatedListeners.remove(listener);
		}
	}
	
	public FileDigestResult digestFile(File file) throws IOException {
		FileDigest fileDigest = FileDigest.getInstance();
		InputStream input = null;
		FileDigestResult result = null;
		try {
			input = new FileInputStream(file);
			byte[] buffer = new byte[BUFFER_LENGTH];
			int readLength = 0;
			long updatedLength = 0;
			while (!isCanceled && (readLength = input.read(buffer, 0, BUFFER_LENGTH)) > 0) {
				fileDigest.update(buffer, 0, readLength);
				
				updatedLength += readLength;
				onProgressUpdated(updatedLength);
			}
			
			if (!isCanceled) {
				result = fileDigest.digest();
			}
		} finally {
			if (input != null) {
				input.close();
			}
		}
		return result;
	}	

	public void cancel() {
		isCanceled = true;
	}
	
	public boolean isCanceled() {
		return isCanceled;
	}

	protected void onProgressUpdated(long processed) {
		if (progressUpdatedListeners != null) {
			ProgressUpdatedEvent event = new ProgressUpdatedEvent(this);
			event.setProcessed(processed);
			for (ProgressUpdatedListener listener : progressUpdatedListeners) {
				listener.updateProgress(event);
			}
		}
	}
}
