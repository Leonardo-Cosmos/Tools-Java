package com.lanmessager.communication.host;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class HostInfo {
	private static final Logger LOGGER = Logger.getLogger(HostInfo.class.getSimpleName());
	
	public static HostInfo LocalHost;
	
	private String name;
	
	private String address;
	
	public String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	void setAddress(String address) {
		this.address = address;
	}

	private HostInfo() {
		
	}
	
	public static HostInfo[] getAllLocalHostInfo() {
		List<HostInfo> hostInfoList = new ArrayList<>();
		try {
			String hostName = InetAddress.getLocalHost().getHostName();
			InetAddress[] addresses = InetAddress.getAllByName(hostName);
			for (InetAddress address : addresses) {
				HostInfo hostInfo = new HostInfo();
				hostInfo.setName(hostName);
				hostInfo.setAddress(address.getHostAddress());
				hostInfoList.add(hostInfo);
			}
		} catch (UnknownHostException ex) {
			LOGGER.error("Failed to get local host addresses.", ex);
		}
		HostInfo[] hostInfos = new HostInfo[hostInfoList.size()];
		hostInfoList.toArray(hostInfos);
		return hostInfos;
	}
}
