package com.lanmessager.communication;

import java.util.EventListener;

public interface ProgressUpdatedListener extends EventListener {
	void updateProgress(ProgressUpdatedEvent e);
}
