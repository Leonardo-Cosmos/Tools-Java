package com.lanmessager.worker;

import java.util.EventListener;

public interface FileCompletedListener extends EventListener {
	void complete(FileCompletedEvent e);
}
