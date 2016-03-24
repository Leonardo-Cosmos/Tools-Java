package com.lanmessager.communication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.lanmessager.file.FileDigest;
import com.lanmessager.file.FileDigestResult;

public class TransferFileClient {
	private Logger LOGGER = Logger.getLogger(TransferFileClient.class.getSimpleName());
	
	private static final int BUFFER_LENGTH = 0x1000;
	
	private boolean isCanceled = false;
	
	private FileDigestResult fileDigestResult;
	
	private Set<ProgressUpdatedListener> progressUpdatedListeners;
		
	public FileDigestResult getFileDigestResult() {
		return fileDigestResult;
	}

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

	public void send(String remoteHost, int port, File file, long fileSize)
			throws IOException, FileUncompletedException {
		Socket socket = null;

		InputStream input = null;
		OutputStream output = null;
		try {			
			socket = new Socket(remoteHost, port);
			output = socket.getOutputStream();
			
			FileDigest fileDigest = FileDigest.getInstance();
			input = new FileInputStream(file);
			byte[] buffer = new byte[BUFFER_LENGTH];
			int readLength = 0;
			long transferedLength = 0;
			while (!isCanceled && (readLength = input.read(buffer, 0, BUFFER_LENGTH)) > 0) {
				output.write(buffer, 0, readLength);
				fileDigest.update(buffer, 0, readLength);
				
				transferedLength += readLength;
				LOGGER.debug("Transfer " + transferedLength + " bytes (total " + fileSize + " bytes).");
				
				onProgressUpdated(transferedLength, fileSize);
			}
			
			if (transferedLength == fileSize) {
				fileDigestResult = fileDigest.digest();
			} else if (isCanceled) {
				
			} else {
				FileUncompletedException ex = new FileUncompletedException(transferedLength, fileSize,
						"Sending file is uncompleted.");
				throw ex;
			}
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException ex) {
					LOGGER.error("Failed to close file input stream.", ex);
				}
			}
			
			if (output != null) {
				try {

					output.close();
				} catch (IOException ex) {
					LOGGER.error("Failed to close socket output stream.", ex);
				}
			}			
			
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException ex) {
					socket = null;
					LOGGER.error("Failed to close socket to server.", ex);
				}
			}
		}
	}
	
	public void cancel() {
		isCanceled = true;
	}
	
	protected void onProgressUpdated(long processed, long total) {
		if (progressUpdatedListeners != null) {
			ProgressUpdatedEvent event = new ProgressUpdatedEvent(this);
			event.setProcessed(processed);
			event.setTotal(total);
			for (ProgressUpdatedListener listener : progressUpdatedListeners) {
				listener.updateProgress(event);
			}
		}
	}
}
