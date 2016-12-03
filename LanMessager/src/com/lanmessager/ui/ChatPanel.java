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
	
	private Set<ProcessPanel> taskPanelSet = new HashSet<>();

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
		
		if (panel instanceof ProcessPanel) {
			taskPanelSet.add((ProcessPanel) panel);
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
		final Set<ProcessPanel> removeSet = new HashSet<>(); 
		for (ProcessPanel panel : taskPanelSet) {
			String status = panel.getStatus();
			if (ProcessPanel.STATUS_ABORT.equals(status) ||
					ProcessPanel.STATUS_CANCEL.equals(status) ||
					ProcessPanel.STATUS_SUCCEED.equals(status) ||
					ProcessPanel.STATUS_FAIL.equals(status)) {
				if (panel instanceof FileProcessPanel) {
					FileProcessPanel fileProcessPanel = (FileProcessPanel) panel;
					LOGGER.debug("To remove " + fileProcessPanel.fileName);	
				} else if (panel instanceof DirProcessPanel) {
					DirProcessPanel dirProcessPanel = (DirProcessPanel) panel;
					LOGGER.debug("To remove " + dirProcessPanel.dirName);
				}
				removeSet.add(panel);
			}
		}
		
		// Remove found out panels.
		if (removeSet.size() > 0) {
			for (ProcessPanel panel : removeSet) {
				if (panel instanceof FileProcessPanel) {
					FileProcessPanel fileProcessPanel = (FileProcessPanel) panel;
					LOGGER.debug("Remove " + fileProcessPanel.fileName);	
				} else if (panel instanceof DirProcessPanel) {
					DirProcessPanel dirProcessPanel = (DirProcessPanel) panel;
					LOGGER.debug("Remove " + dirProcessPanel.dirName);
				}
				remove(panel);
				remove(panel.getVerticalStrut());
				taskPanelSet.remove(panel);
			}
			updateUI();
		}
	}
	
}
