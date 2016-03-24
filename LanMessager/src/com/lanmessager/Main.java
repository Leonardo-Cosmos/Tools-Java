package com.lanmessager;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.log4j.PropertyConfigurator;

import com.lanmessager.ui.MainFrame;

public class Main {

	public static void main(String[] args) {
		PropertyConfigurator.configure("log4j.properties");
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				JFrame mainFrame = new MainFrame();
				mainFrame.setVisible(true);
			}
		});
		
	}

}
