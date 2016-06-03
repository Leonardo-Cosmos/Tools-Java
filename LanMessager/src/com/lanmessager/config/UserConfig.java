package com.lanmessager.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class UserConfig {
	
	private static final Logger LOGGER = Logger.getLogger(UserConfig.class.getSimpleName());
	
	private static final String PROP_FILE_PATH = "UserConfig.properties";
	private static final String PROP_WINDOW_SIZE_WIDTH = "WindowSizeWidth";
	private static final String PROP_WINDOW_SIZE_HEIGHT = "WindowSizeHeight";
	private static final String PROP_WINDOW_LOCATION_X = "WindowLocationX";
	private static final String PROP_WINDOW_LOCATION_Y = "WindowLocationY";
	private static final String PROP_SPLIT_PANE_DIVIDER_LOCATION = "SplitPaneDividerLocation";
	private static final String PROP_OPEN_FILE_PATH = "OpenFilePath";
	private static final String PROP_SAVE_FILE_PATH = "SaveFilePath";
	
	private static final String DEFAULT_SIZE_WIDTH = "800";
	private static final String DEFAULT_SIZE_HEIGHT = "600";
	private static final String DEFAULT_LOCATION_X = "200";
	private static final String DEFAULT_LOCATION_Y = "100";
	private static final String DEFAULT_SPLIT_PANE_DIVIDER_LOCATION = "100";

	public static UserConfig instance = new UserConfig();
	
	private int windowSizeWidth;
	private int windowSizeHeight;
	private int windowLocationX;
	private int windowLocationY;
	private int splitPaneDividerLocation;
	private String openFilePath;
	private String saveFilePath;
	
	private UserConfig() {
		
	}
	
	public void save() throws IOException {
		/* Saves properties to object. */
		Properties properties = new Properties();
		
		properties.setProperty(PROP_WINDOW_SIZE_WIDTH, Integer.toString(windowSizeWidth));
		properties.setProperty(PROP_WINDOW_SIZE_HEIGHT, Integer.toString(windowSizeHeight));
		
		properties.setProperty(PROP_WINDOW_LOCATION_X, Integer.toString(windowLocationX));
		properties.setProperty(PROP_WINDOW_LOCATION_Y, Integer.toString(windowLocationY));
		
		properties.setProperty(PROP_SPLIT_PANE_DIVIDER_LOCATION, Integer.toString(splitPaneDividerLocation));
		
		properties.setProperty(PROP_OPEN_FILE_PATH, openFilePath);
		properties.setProperty(PROP_SAVE_FILE_PATH, saveFilePath);
		
		/* Saves properties to file. */
		File propFile = new File(PROP_FILE_PATH);
		try {
			if (!propFile.exists()) {
				propFile.createNewFile();
			}
			OutputStream output = new FileOutputStream(propFile);
			properties.store(output, null);
		} catch (IOException ex) {
			LOGGER.error("Save user configure failed.", ex);
			throw ex;
		}
	}
	
	public void load() throws IOException {
		/* Loads properties from file. */
		Properties properties = new Properties();
		
		File propFile = new File(PROP_FILE_PATH);
		if (propFile.exists()) {
			try {
				FileInputStream input = new FileInputStream(propFile);
				properties.load(input);
			} catch (IOException ex) {
				LOGGER.error("Load user configure failed.", ex);
				throw ex;
			}
		}
		
		/* Loads properties from object. */
		try {
			windowSizeWidth = Integer.parseInt(properties.getProperty(PROP_WINDOW_SIZE_WIDTH, DEFAULT_SIZE_WIDTH));
			windowSizeHeight = Integer.parseInt(properties.getProperty(PROP_WINDOW_SIZE_HEIGHT, DEFAULT_SIZE_HEIGHT));
		} catch (NumberFormatException ex) {
			LOGGER.error("Parse size properties failed.", ex);

			windowSizeWidth = Integer.parseInt(DEFAULT_SIZE_WIDTH);
			windowSizeHeight = Integer.parseInt(DEFAULT_SIZE_HEIGHT);
		}

		try {
			windowLocationX = Integer.parseInt(properties.getProperty(PROP_WINDOW_LOCATION_X, DEFAULT_LOCATION_X));
			windowLocationY = Integer.parseInt(properties.getProperty(PROP_WINDOW_LOCATION_Y, DEFAULT_LOCATION_Y));
		} catch (NumberFormatException ex) {
			LOGGER.error("Parse location properties failed.", ex);

			windowLocationX = Integer.parseInt(DEFAULT_LOCATION_X);
			windowLocationY = Integer.parseInt(DEFAULT_LOCATION_Y);
		}

		try {
			splitPaneDividerLocation = Integer.parseInt(properties.getProperty(PROP_SPLIT_PANE_DIVIDER_LOCATION,
					DEFAULT_SPLIT_PANE_DIVIDER_LOCATION));
		} catch (NumberFormatException ex) {
			LOGGER.error("Parse split pane property failed.", ex);
			splitPaneDividerLocation = Integer.parseInt(DEFAULT_SPLIT_PANE_DIVIDER_LOCATION);
		}
		
		openFilePath = properties.getProperty(PROP_OPEN_FILE_PATH);
		saveFilePath = properties.getProperty(PROP_SAVE_FILE_PATH);
	}

	public int getWindowSizeWidth() {
		return windowSizeWidth;
	}

	public void setWindowSizeWidth(int windowSizeWidth) {
		this.windowSizeWidth = windowSizeWidth;
	}

	public int getWindowSizeHeight() {
		return windowSizeHeight;
	}

	public void setWindowSizeHeight(int windowSizeHeight) {
		this.windowSizeHeight = windowSizeHeight;
	}

	public int getWindowLocationX() {
		return windowLocationX;
	}

	public void setWindowLocationX(int windowLocationX) {
		this.windowLocationX = windowLocationX;
	}

	public int getWindowLocationY() {
		return windowLocationY;
	}

	public void setWindowLocationY(int windowLocationY) {
		this.windowLocationY = windowLocationY;
	}

	public int getSplitPaneDividerLocation() {
		return splitPaneDividerLocation;
	}

	public void setSplitPaneDividerLocation(int splitPaneDividerLocation) {
		this.splitPaneDividerLocation = splitPaneDividerLocation;
	}

	public String getOpenFilePath() {
		return openFilePath;
	}

	public void setOpenFilePath(String openFilePath) {
		this.openFilePath = openFilePath;
	}

	public String getSaveFilePath() {
		return saveFilePath;
	}

	public void setSaveFilePath(String saveFilePath) {
		this.saveFilePath = saveFilePath;
	}
}
