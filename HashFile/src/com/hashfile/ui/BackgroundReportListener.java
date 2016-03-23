package com.hashfile.ui;

import java.util.EventListener;

public interface BackgroundReportListener extends EventListener {
	void report(BackgroundReportEvent e);
}
