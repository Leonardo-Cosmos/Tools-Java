package com.hashfile.ui;

import java.util.EventListener;

public interface BackgroundCompleteListener extends EventListener {
	void complete(BackgroundCompleteEvent e);
}
