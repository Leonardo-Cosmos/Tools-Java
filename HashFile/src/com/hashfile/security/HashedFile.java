package com.hashfile.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.event.EventListenerList;

import com.hashfile.ui.CancellationChecker;

/**
 * Represents a file and its hash values.
 * @author Leonardo
 *
 */
public class HashedFile {
	
	private static final int FILE_BUFFER_LENGTH = 0x1000;
	
	private File file;
	
	private Hash hash;
	
	private CancellationChecker cancellationChecker;
	
	private EventListenerList listenerList;
	
	/**
	 * Constructs a new HashedFile with specified file.
	 * @param file the file to be hashed.
	 */
	public HashedFile(File file) {
		this(file, null);
	}
	
	/**
	 * Constructs a new HashedFile with specified file and CancellationChecker.
	 * @param file the file to be hashed.
	 * @param checker the CancellationChecker used to determine whether the calculation should be cancel.
	 */
	public HashedFile(File file, CancellationChecker checker) {
		this.file = file;
		this.hash = null;
		this.cancellationChecker = checker;
		
		listenerList = new EventListenerList();
	}

	/**
	 * Reads file and calculates its hash value.
	 * @throws FileNotFoundException if the file does not exist,
	 *  is a directory rather than a regular file, or for some other
	 *   reason cannot be opened for reading.
	 * @throws IOException if an I/O error occurs.
	 */
	public void calculateHash() throws FileNotFoundException,
			IOException {
		FileInputStream fs = new FileInputStream(file);
		hash = new Hash();
		
		long updatedLength = 0;
		long fileLength = file.length();
		int readCount = 0;
		byte[] buffer = new byte[FILE_BUFFER_LENGTH];
		while (true) {
			if (cancellationChecker != null && cancellationChecker.isCancelled()) {
				break;
			}
			
			readCount = fs.read(buffer);
			if (readCount != -1) {
				hash.update(buffer, 0 ,readCount);
				updatedLength = updatedLength + readCount;
				fireProgressChanged(new HashProgressChangedEvent(
						this, updatedLength, fileLength));
			} else {
				break;
			}
		}
		
		fs.close();
	}
	
	/**
	 * Returns the file which is hashed.
	 */
	public File getFile() {
		return file;
	}
	
	/**
	 * Returns the hash value of the file.
	 */
	public Hash getHash() {
		return hash;
	}
	
	public CancellationChecker getCancellationChecker() {
		return cancellationChecker;
	}

	public void setCancellationChecker(CancellationChecker cancellationChecker) {
		this.cancellationChecker = cancellationChecker;
	}

	/**
	 * Adds a HashProgressChangedListener to the object.
	 * @param l the listener to be added.
	 */
	public void addProgressChangedListener(HashProgressChangedListener l) {
		listenerList.add(HashProgressChangedListener.class, l);
	}
	
	/**
	 * Removes a HashProgressChangedListener from the object.
	 * @param l the listener to be removed.
	 */
	public void removeProgressChangedListener(HashProgressChangedListener l) {
		listenerList.remove(HashProgressChangedListener.class, l);
	}
	
	protected void fireProgressChanged(HashProgressChangedEvent e) {
		HashProgressChangedListener[] listeners = 
			listenerList.getListeners(HashProgressChangedListener.class);
		
		for (HashProgressChangedListener listener : listeners) {
			listener.Report(e);
		}
	}
}
