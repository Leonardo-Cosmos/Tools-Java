package com.lanmessager.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.lanmessager.net.packet.Packet;
import com.lanmessager.net.packet.PacketFactory;

public class ChatServer {
	private Logger LOGGER = Logger.getLogger(ChatServer.class.getSimpleName());

	public static final int PORT_CHAT = 12001;
	
	private ServerSocket serverSocket;

	private boolean isRunning = false;

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

	public ChatServer() {

	}

	public void start() {
		isRunning = true;

		try {
			serverSocket = new ServerSocket(PORT_CHAT);
			while (true) {
				Socket client = serverSocket.accept();
				handleClientSocket(client);
			}
		} catch (SocketException ex) {
			LOGGER.info("Socket is interruptted.", ex);
		} catch (IOException ex) {
			LOGGER.error("Error when hanlde chat message.", ex);
		} finally {
			if (isRunning) {
				stop();
			}
		}

		LOGGER.info("Chat server is listening " + PORT_CHAT + ".");
	}

	public void stop() {
		if (isRunning) {
			isRunning = false;
			try {
				if (serverSocket != null) {
					serverSocket.close();
					serverSocket = null;
				}
			} catch (IOException ex) {
				LOGGER.error("Failed to close socket.", ex);
			}
		}
	}

	public boolean isRunning() {
		return isRunning;
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

	private void handleClientSocket(Socket socket) {
		try {
			DataInputStream input = new DataInputStream(socket.getInputStream());
			String packetString = input.readUTF();

			LOGGER.debug(packetString);
			Packet packet = PacketFactory.parse(packetString);
			onNewPacket(packet);

			DataOutputStream out = new DataOutputStream(socket.getOutputStream());

			out.close();
			input.close();
		} catch (IOException ex) {
			LOGGER.error("Failed to handle socket from client.", ex);
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException ex) {
					socket = null;
					LOGGER.error("Failed to close socket from client.", ex);
				}
			}
		}
	}

	/*
	 * private class ChatHandler implements Runnable { private Socket socket;
	 * 
	 * public ChatHandler(Socket client) { this.socket = client; }
	 * 
	 * @Override public void run() { handleClientSocket(socket); } }
	 */
}
