package com.lanmessager.communication.host;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public final class HostInfoHelper {
	private static final Logger LOGGER = Logger.getLogger(HostInfoHelper.class.getSimpleName());
	
	public static final String BROADCAST_ADRESS = "255.255.255.255";
	
	public static String getLocalHostName() {
		String hostName = null;
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException ex) {
			LOGGER.error("Failed to get local host name.", ex);
		}
		return hostName;
	}
	
	public static String getLocalHostAddress() {
		String hostName = null;
		try {
			hostName = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException ex) {
			LOGGER.error("Failed to get local host name.", ex);
		}
		return hostName;
	}
	
	public static String[] getLocalHostAddresses() {
		List<String> addressList = new ArrayList<>();
		try {
			String hostName = getLocalHostName();
			InetAddress[] addresses = InetAddress.getAllByName(hostName);
			for (InetAddress address : addresses) {
				if (address instanceof Inet4Address) {
					addressList.add(address.getHostAddress());
				}
			}
		} catch (UnknownHostException ex) {
			LOGGER.error("Failed to get local host.", ex);
		}
		String[] addresses = new String[addressList.size()];
		return addressList.toArray(addresses);
	}
	
	public static String getInetAddress(String host) {
		String hostAddress = null;
		try {
			hostAddress = InetAddress.getByName(host).getHostAddress();
		} catch (UnknownHostException ex) {
			LOGGER.warn("IP address for the host (" + host + ") could be found.", ex);			
		}
		return hostAddress;
	}
}
