package com.lanmessager.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.apache.log4j.Logger;

import com.lanmessager.net.message.Message;
import com.lanmessager.net.packet.Packet;
import com.lanmessager.net.packet.PacketFactory;

public class ChatClient {
	private Logger LOGGER = Logger.getLogger(ChatClient.class.getSimpleName());

	public ChatClient() {
		
	}

	public void send(String remoteHost, Message message) throws IOException {
		Socket socket = null;

		try {
			socket = new Socket(remoteHost, ChatServer.PORT_CHAT);

			DataInputStream input = new DataInputStream(socket.getInputStream());
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			
			Packet packet = PacketFactory.createPacket(message);			
			String packetString = PacketFactory.toString(packet);
			LOGGER.debug(packetString);
			
			output.writeUTF(packetString);
			
			output.close();			
			input.close();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException ex) {
					socket = null;
					LOGGER.error("Failed to close socket to server.", ex);
				}
			}
		}
	}
}
