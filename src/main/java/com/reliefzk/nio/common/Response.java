package com.reliefzk.nio.common;

import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.io.IOException;

/**
 * @author ReliefZk
 */

public class Response {
	private SocketChannel sc;

	public Response(SocketChannel sc) {
		this.sc = sc;
	}

	/**
	 * 
	 * @param data
	 */
	public void send(byte[] data) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(data.length);
		buffer.put(data, 0, data.length);
		buffer.flip();
		sc.write(buffer);
	}
}
