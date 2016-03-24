package com.lanmessager.backgroundworker;

import java.util.EventListener;

public interface FileProgressUpdatedListener extends EventListener {
	void updateProgress(FileProgressUpdatedEvent e);
}
