package com.lanmessager.ui;

import java.awt.Component;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import org.apache.log4j.Logger;

public class ChatPanel extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8469171970119713660L;

	private static final Logger LOGGER = Logger.getLogger(ChatPanel.class.getSimpleName());

	private static final int VERTICAL_STRUT_HEIGHT = 10;
	
	private Set<FileProcessPanel> taskPanelSet = new HashSet<>();

	public ChatPanel() {
		initComponents();
	}
	
	private void initComponents() {
		BoxLayout boxLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(boxLayout);
	}
	
	public void addPanel(ChatMessagePanel panel) {
		if (panel == null) {
			return;
		}
		
		if (panel instanceof FileProcessPanel) {
			taskPanelSet.add((FileProcessPanel) panel);
		}
		
		add(panel);
		// Add a vertical strut after the panel. Set it as ChatPanel's property.
		Component verticalStrut = Box.createVerticalStrut(VERTICAL_STRUT_HEIGHT);
		panel.setVerticalStrut(verticalStrut);
		add(verticalStrut);
		
		updateUI();
	}
	
	public void removePanel(ChatMessagePanel panel) {
		if (panel == null) {
			return;
		}
		
		remove(panel);
		// Remove the vertical strut after the panel. Get it from ChatPanel's property.
		Component verticalStrut = panel.getVerticalStrut();
		remove(verticalStrut);
		
		updateUI();
		
		if (panel instanceof FileProcessPanel) {
			taskPanelSet.remove((FileProcessPanel) panel);
		}
	}
	
	public void clearCompletedTaskPanel() {
		// Find out panels to be removed.
		final Set<FileProcessPanel> removeSet = new HashSet<>(); 
		for (FileProcessPanel panel : taskPanelSet) {
			String status = panel.getStatus();
			if (FileProcessPanel.STATUS_ABORT.equals(status) ||
					FileProcessPanel.STATUS_CANCEL.equals(status) ||
					FileProcessPanel.STATUS_SUCCEED.equals(status) ||
					FileProcessPanel.STATUS_FAIL.equals(status)) {
				LOGGER.debug("To remove " + panel.fileName);
				removeSet.add(panel);
			}
		}
		
		// Remove found out panels.
		if (removeSet.size() > 0) {
			for (FileProcessPanel panel : removeSet) {
				LOGGER.debug("Remove " + panel.fileName);
				remove(panel);
				remove(panel.getVerticalStrut());
				taskPanelSet.remove(panel);
			}
			updateUI();
		}
	}
	
}
