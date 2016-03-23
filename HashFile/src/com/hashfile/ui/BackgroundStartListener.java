package com.hashfile.ui;

import java.util.EventListener;

public interface BackgroundStartListener extends EventListener {
	void start(BackgroundStartEvent e) throws Throwable;
}
