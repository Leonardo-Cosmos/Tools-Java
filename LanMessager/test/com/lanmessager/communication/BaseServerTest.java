package com.lanmessager.communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.lanmessager.BaseTest;

public class BaseServerTest extends BaseTest {

	private static final Logger LOGGER = Logger.getLogger(BaseServerTest.class.getSimpleName());
	
	protected void holdServer() {
		//String outputStr = "";
		try {
			new BufferedReader(new InputStreamReader(System.in)).readLine();
		} catch (IOException ex) {
			LOGGER.error("Error when read standard input.", ex);
		}
	}
}
