package com.hashfile.ui;

import javax.swing.event.EventListenerList;

public class BackgroundWorker extends Thread {

	private EventListenerList listenerList;
	
	private Object startParameter; 
	
	public BackgroundWorker() {
		listenerList = new EventListenerList();
	}
	
	@Override
	public void run() {
		BackgroundStartEvent startEvent = null;
		Throwable error = null;
		try {
			startEvent = new BackgroundStartEvent(this, startParameter);
			fireStart(startEvent);
		} catch (Throwable ex) {
			error = ex;
		}

		BackgroundCompleteEvent completeEvent = 
			new BackgroundCompleteEvent(this, startEvent.getResult(), error);
		fireComplete(completeEvent);
	}
	
	public void reportProgress(Object userStage) {
		fireReport(new BackgroundReportEvent(this, userStage));
	}
	
	@Override
	public void start() {
		super.start();
	}
	
	public void start(Object parameter) {
		startParameter = parameter;
		super.start();
	}
	
	public void addStartListener(BackgroundStartListener l) {
		listenerList.add(BackgroundStartListener.class, l);
	}
	
	public void addReportListener(BackgroundReportListener l) {
		listenerList.add(BackgroundReportListener.class, l);
	}
	
	public void addCompleteListener(BackgroundCompleteListener l) {
		listenerList.add(BackgroundCompleteListener.class, l);
	}	

	public void removeStartListener(BackgroundStartListener l) {
		listenerList.remove(BackgroundStartListener.class, l);
	}	

	public void removeReportListener(BackgroundReportListener l) {
		listenerList.remove(BackgroundReportListener.class, l);
	}	

	public void removeCompleteListener(BackgroundCompleteListener l) {
		listenerList.remove(BackgroundCompleteListener.class, l);
	}
	
	protected void fireStart(BackgroundStartEvent e) throws Throwable {
		BackgroundStartListener[] listeners = 
			listenerList.getListeners(BackgroundStartListener.class);
		
		for (BackgroundStartListener listener : listeners) {
			listener.start(e);
		}
	}
	
	protected void fireReport(BackgroundReportEvent e) {
		BackgroundReportListener[] listeners = 
			listenerList.getListeners(BackgroundReportListener.class);

		for (BackgroundReportListener listener : listeners) {
			listener.report(e);
		}
	}
	
	protected void fireComplete(BackgroundCompleteEvent e) {
		BackgroundCompleteListener[] listeners = 
			listenerList.getListeners(BackgroundCompleteListener.class);
		
		for (BackgroundCompleteListener listener : listeners) {
			listener.complete(e);
		}
	}
}