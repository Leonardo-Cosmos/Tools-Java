package com.lanmessager.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.lanmessager.net.packet.Packet;
import com.lanmessager.net.packet.PacketFactory;

public class NotifyServer {
	private Logger LOGGER = Logger.getLogger(NotifyServer.class.getSimpleName());

	public static final int PORT_NOTIFY = 12000;
	
	private static final int BUFFER_LENGTH = 0x400;

	private DatagramSocket socket;

	private boolean isRunning = false;

	private byte[] buffer = new byte[BUFFER_LENGTH];
	private Set<NewPacketListener> newPacketListeners;

	public void addNewMessageListener(NewPacketListener listener) {
		if (newPacketListeners == null) {
			newPacketListeners = new HashSet<>();
		}
		newPacketListeners.add(listener);
	}
	
	public void removeNewMessageListener(NewPacketListener listener) {
		if (newPacketListeners != null) {
			newPacketListeners.remove(listener);
		}
	}

	public NotifyServer() {

	}

	public void start() {
		isRunning = true;

		try {
			socket = new DatagramSocket(PORT_NOTIFY);
			while (true) {

				DatagramPacket datagramPacket = new DatagramPacket(buffer, BUFFER_LENGTH);
				socket.receive(datagramPacket);

				String packetString = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
				LOGGER.debug(packetString);

				Packet packet = PacketFactory.parse(packetString);

				onNewPacket(packet);
			}
		} catch (SocketException ex) {
			LOGGER.info("Socket is interruptted.", ex);
		} catch (IOException ex) {
			LOGGER.error("Error when handle notify message.", ex);
		} finally {
			if (isRunning) {
				stop();
			}
		}

		LOGGER.info("Notify server is listening " + PORT_NOTIFY + ".");
	}

	public void stop() {
		if (isRunning) {
			isRunning = false;
			if (socket != null) {
				socket.close();
				socket = null;
			}
		}
	}

	protected void onNewPacket(Packet packet) {
		if (newPacketListeners != null) {
			NewPachetEvent event = new NewPachetEvent(this);
			event.setPacket(packet);
			for (NewPacketListener listener : newPacketListeners) {
				listener.handlePacket(event);
			}
		}
	}
}
