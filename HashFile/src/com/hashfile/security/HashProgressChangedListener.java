package com.hashfile.security;

import java.util.EventListener;

/**
 * The listener interface for receiving hash progress changed events.
 * @author Leonardo
 *
 */
public interface HashProgressChangedListener extends EventListener {
	
	/**
	 * Invoked when the hash progress has been changed.
	 */
	void Report(HashProgressChangedEvent e);
}
