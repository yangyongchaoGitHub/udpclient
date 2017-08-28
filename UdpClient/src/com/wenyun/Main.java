package com.wenyun;

import java.io.Console;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.Set;

public class Main {
	private static String ip = "114.115.143.70";
	private static int port = 10561;
	private static DatagramChannel sendChannel;
	private static Selector selector;

	public static void main(String[] args) {
		try {
			sendChannel = DatagramChannel.open();
			sendChannel.configureBlocking(false);
			selector = Selector.open();
			SocketAddress target = new InetSocketAddress(ip, port);
			sendChannel.connect(target);
			sendChannel.register(selector, SelectionKey.OP_READ);
		} catch (Exception e) {
			e.printStackTrace();
		}

		/*if (socket == null) {
			System.out.println("create socket error");
			System.exit(0);
		}*/

		LoopThread loopThread = new LoopThread(sendChannel, selector);
		loopThread.start();

		Scanner scanner = new Scanner(System.in);
		while (true) {
			String str = scanner.nextLine();
			System.out.println("System.in " + str);
			if ("1".equals(str)) {
				byte[] test = new byte[1];
				test[0] = 0x01;
				
				ByteBuffer b = ByteBuffer.allocate(1);
				b.put(test);
				loopThread.send(b);

			} else if ("2".equals(str)) {
				byte[] test = new byte[1];
				test[0] = 0x02;
				ByteBuffer b = ByteBuffer.allocate(1);
				b.put(test);

				loopThread.send(b);
			} else {
			}
		}
	}

	private static String getHex(byte[] bytes) {
		String result = "";
		for (byte b : bytes) {
			String str = Integer.toHexString(b & 0xFF);
			if (str.length() == 1) {
				str = '0' + str;
			}
			result += str;
		}
		return result;
	}
}

class LoopThread extends Thread {
	DatagramChannel send;
	Selector selector;

	public LoopThread(DatagramChannel send, Selector selector) {
		this.send = send;
		this.selector = selector;
	}

	@Override
	public void run() {
		while (true) {
			int nKeys = 0;
			try {
				Thread.sleep(50);
				nKeys = selector.select();
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (nKeys > 0) {
				Set<SelectionKey> keys = selector.selectedKeys();
				for (SelectionKey key : keys) {
					System.out.println(key.isConnectable() + " " + key.isWritable() + " " + key.isReadable() + " "
							+ key.isAcceptable());
					if (key.isConnectable()) {
						try {
							System.out.println("key.isConnectable()");
							send.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
						} catch (ClosedChannelException e) {
							e.printStackTrace();
						}
					}
					if (key.isReadable()) {
						// 有流可读取
						System.out.println("test 2");
						ByteBuffer buffer = ByteBuffer.allocate(10);
						DatagramChannel sc = (DatagramChannel) key.channel();
						int readBytes = 0;

						try {
							int ret = 0;
							try {
								while ((ret = sc.read(buffer)) > 0) {
									readBytes += ret;
								}
								System.out.println(buffer.array().length + new String(buffer.array()));
								send.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
							} catch (Exception e) {

							} finally {
								buffer.flip();
							}
						} finally {
							if (buffer != null) {
								buffer.clear();
							}
						}
					}
				}
			}
		}
	}

	public void send(ByteBuffer buffer) {
		buffer.flip();
		try {
			int writeenedSize = send.write(buffer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
