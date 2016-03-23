package com.hashfile.security;

import java.util.EventObject;

/**
 * An event which indicates that hash file progress has changed.
 * @author Leonardo
 *
 */
public class HashProgressChangedEvent extends EventObject {

	/**
	 * Serial version UID of this class.
	 */
	private static final long serialVersionUID = -2476409293255457698L;

	private long updatedLength;
	
	private long fileLength;
	
	/**
	 * Constructs a HashProgressChangedEvent object with specified source component, updated length, file length.
	 * @param source - the component that originated the event.
	 * @param updatedLength - the length of bytes which has been updated. 
	 * @param fileLength - the total length of the file which is being hashed.
	 */
	public HashProgressChangedEvent(Object source, long updatedLength, long fileLength) {
		super(source);
		
		this.updatedLength = updatedLength;
		this.fileLength = fileLength;
	}
	
	/**
	 * Returns the length of bytes which has been updated.
	 * @return
	 */
	public long getUpdatedLength() {
		return updatedLength;
	}

	/**
	 * Returns the total length of the file which is being hashed.
	 * @return
	 */
	public long getFileLength() {
		return fileLength;
	}
}
