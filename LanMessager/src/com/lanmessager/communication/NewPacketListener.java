package com.lanmessager.communication;

import java.util.EventListener;

@FunctionalInterface
public interface NewPacketListener extends EventListener {
	void handlePacket(NewPachetEvent e);
}
