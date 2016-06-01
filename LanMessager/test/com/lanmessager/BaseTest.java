package com.lanmessager;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;

public class BaseTest {
	
	@Before
	public void setUp() {
		PropertyConfigurator.configure("log4j.properties");
	}

	protected void printException(Throwable throwable) {
		throwable.printStackTrace();
	}
	
	protected void printException(String message, Throwable throwable) {
		System.out.println(message);
		throwable.printStackTrace();
	}
}
