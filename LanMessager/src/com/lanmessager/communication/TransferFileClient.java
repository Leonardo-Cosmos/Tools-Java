package com.lanmessager.communication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.apache.log4j.Logger;

import com.lanmessager.backgroundworker.FileProgress;
import com.lanmessager.concurrent.Task;
import com.lanmessager.concurrent.TaskExecutor;
import com.lanmessager.file.FileDigest;
import com.lanmessager.file.FileDigestResult;

public class TransferFileClient extends TaskExecutor<String, FileDigestResult, FileProgress> {
	private Logger LOGGER = Logger.getLogger(TransferFileClient.class.getSimpleName());
	
	public static final byte ID_END = '\0';
	
	private static final int BUFFER_LENGTH = 0x1000;
	
	private static final int THREAD_NUMBER = 2;
	
	public TransferFileClient() {
		super(THREAD_NUMBER);
	}

	/**
	 * Start sending a file asynchronously.
	 * 
	 */
	public void send(String fileId, String remoteHost, File file, long fileSize) {
		SendFileTask task = new SendFileTask(fileId, remoteHost, file, fileSize);
		submit(fileId, task);
	}

	private class SendFileTask extends Task<FileDigestResult, FileProgress> {
		private final String fileId;
		private final String remoteHost;
		private final File file;
		private final long fileSize;

		public SendFileTask(String fileId, String remoteHost, File file, long fileSize) {
			this.fileId = fileId;
			this.remoteHost = remoteHost;
			this.file = file;
			this.fileSize = fileSize;
		}

		@Override
		public FileDigestResult execute() throws Exception {
			return transfer(fileId, remoteHost, file, fileSize);
		}
		
		private FileDigestResult transfer(String fileId, String remoteHost, File file, long fileSize)
				throws IOException, FileUncompletedException {
			Socket socket = null;
			InputStream input = null;
			OutputStream output = null;
			FileDigestResult fileDigestResult = null;
			try {
				socket = new Socket(remoteHost, TransferFileServer.PORT_TRANSFER_FILE);
				output = socket.getOutputStream();
				
				/* Write ID to head of output. */
				output.write(fileId.getBytes());
				output.write(new byte[] {ID_END});

				FileDigest fileDigest = FileDigest.getInstance();
				input = new FileInputStream(file);
				byte[] buffer = new byte[BUFFER_LENGTH];
				int readLength = 0;
				long transferedLength = 0;
				
				/* Send real file. */
				while (!isCancelled() && (readLength = input.read(buffer, 0, BUFFER_LENGTH)) != -1) {
					output.write(buffer, 0, readLength);
					fileDigest.update(buffer, 0, readLength);
					transferedLength += readLength;
					
					LOGGER.debug("Transfer " + transferedLength + " bytes (total " + fileSize + " bytes).");

					FileProgress progress = new FileProgress();
					progress.setProcessed(transferedLength);
					progress.setTotal(fileSize);
					onStatusUpdated(progress);
				}

				if (transferedLength == fileSize) {
					fileDigestResult = fileDigest.digest();
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
			return fileDigestResult;
		}
	}
}
