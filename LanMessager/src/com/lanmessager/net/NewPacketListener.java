package com.lanmessager.net;

import java.util.EventListener;

@FunctionalInterface
public interface NewPacketListener extends EventListener {
	void handlePacket(NewPachetEvent e);
}
