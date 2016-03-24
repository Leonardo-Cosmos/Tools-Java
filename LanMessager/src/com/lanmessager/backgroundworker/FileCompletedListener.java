package com.lanmessager.backgroundworker;

import java.util.EventListener;

public interface FileCompletedListener extends EventListener {
	void complete(FileCompletedEvent e);
}
