package com.lanmessager.communication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.lanmessager.file.FileDigest;
import com.lanmessager.file.FileDigestResult;

public class TransferFileServer {
	private static final Logger LOGGER = Logger.getLogger(TransferFileServer.class.getSimpleName());
	
	public static final int PORT_TRANSFER_FILE = 12002;

	private static final int BUFFER_LENGTH = 0x1000;

	private ServerSocket serverSocket;

	private boolean isRunning = false;
	
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

	public void start(int port, File file, long fileSize) throws IOException, FileUncompletedException {
		isRunning = true;

		try {
			serverSocket = new ServerSocket(port);

			Socket client = serverSocket.accept();
			handleClientSocket(client, file, fileSize);

		} finally {
			if (isRunning) {
				stop();
			}
		}

		LOGGER.info("Transfer server is listening " + port + ".");
	}

	public void cancel() {
		isCanceled = true;
	}
	
	private void stop() {
		if (isRunning) {
			isRunning = false;
			try {
				if (serverSocket != null) {
					serverSocket.close();
					serverSocket = null;
				}
			} catch (IOException ex) {
				LOGGER.error("Failed to close socket.", ex);
			}
		}
	}

	/*private boolean isRunning() {
		return isRunning;
	}*/

	protected void onProgressUpdated(long processed, long total) {
		if (progressUpdatedListeners != null) {
			for (ProgressUpdatedListener listener : progressUpdatedListeners) {
				ProgressUpdatedEvent event = new ProgressUpdatedEvent(this);
				event.setProcessed(processed);
				event.setTotal(total);
				listener.updateProgress(event);
			}
		}
	}
	
	private void handleClientSocket(Socket socket, File file, long fileSize)
			throws IOException, FileUncompletedException {

		InputStream input = null;
		OutputStream output = null;
		try {
			FileDigest fileDigest = FileDigest.getInstance();
			input = socket.getInputStream();
			output = new FileOutputStream(file);
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
						"Receiving file is uncompleted.");
				throw ex;
			}
		} finally {
			if (output != null) {
				try {

					output.close();
				} catch (IOException ex) {
					LOGGER.error("Failed to close file output stream.", ex);
				}
			}

			if (input != null) {
				try {
					input.close();
				} catch (IOException ex) {
					LOGGER.error("Failed to close socket input stream.", ex);
				}
			}
			
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException ex) {
					socket = null;
					LOGGER.error("Failed to close socket from client.", ex);
				}
			}
		}
	}
}
