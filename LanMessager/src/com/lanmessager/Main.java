package com.lanmessager;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.lanmessager.ui.MainFrame;

public class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class.getSimpleName());
	
	public static void main(String[] args) {
		PropertyConfigurator.configure("log4j.properties");
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException ex) {
			LOGGER.error("Set look and feel failed.", ex);
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				JFrame mainFrame = new MainFrame();
				mainFrame.setVisible(true);
			}
		});
		
	}

}
