package com.nyxus.broker.socket;

import java.nio.ByteBuffer;

public interface ClientEventManager {
	
	void connectionReady(SocketClient client);
	void availableData(SocketClient client,ByteBuffer byteBuffer);
	void connectionClose(SocketClient client);
}
