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
	private static DatagramPacket sendPacket;
	private static DatagramSocket socket;
	private static String ip = "114.115.143.70";
	private static int port = 10561;
	private static DatagramChannel receiveChannel;
	private static DatagramChannel sendChannel;
	private static Selector selector;

	public static void main(String[] args) {
		//ByteBuffer buffer = ByteBuffer.allocate(1024);
		try {
			// receiveChannel = DatagramChannel.open();
			// receiveChannel.configureBlocking(false);
			// socket = receiveChannel.socket();
			// socket.bind(new InetSocketAddress(port));

			// SelectionKey reveive = receiveChannel.register(selector,
			// SelectionKey.OP_READ);

			// 之后可采取和TCP/IP NIO中对selector遍历方式进行流信息的读取

			sendChannel = DatagramChannel.open();
			sendChannel.configureBlocking(false);
			selector = Selector.open();
			SocketAddress target = new InetSocketAddress(ip, port);
			sendChannel.connect(target);
			sendChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

			// 阻塞写入流，若发送缓冲区已满，返回0，此时可继续注册OP_WRITE事件
			// sendChannel.write(buffer);

		} catch (Exception e) {
			socket = null;
			e.printStackTrace();
		}

		/*if (socket == null) {
			System.out.println("create socket error");
			System.exit(0);
		}*/

		LoopThread loopThread = new LoopThread(sendChannel, receiveChannel, selector);
		loopThread.start();

		Scanner scanner = new Scanner(System.in);
		while (true) {
			String str = scanner.nextLine();
			System.out.println("System.in " + str);
			if ("1".equals(str)) {
				byte[] test = new byte[1];
				test[0] = 0x01;
				/*
				 * DatagramPacket sendPacket = null; try { sendPacket = new
				 * DatagramPacket(test, test.length, InetAddress.getByName(ip),
				 * port); } catch (UnknownHostException e) { // TODO
				 * Auto-generated catch block e.printStackTrace(); }
				 */
				ByteBuffer b = ByteBuffer.allocate(1);
				b.put(test);
				// System.out.println("loopThread send 1");
				loopThread.send(b);
				// System.out.println("loopThread send 2");

			} else if ("2".equals(str)) {
				byte[] test = new byte[1];
				test[0] = 0x02;
				/*
				 * DatagramPacket sendPacket = null; try { sendPacket = new
				 * DatagramPacket(test, test.length, InetAddress.getByName(ip),
				 * port); } catch (UnknownHostException e) { // TODO
				 * Auto-generated catch block e.printStackTrace(); }
				 */
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
	DatagramChannel listen;
	Selector selector;

	public LoopThread(DatagramChannel send, DatagramChannel listen, Selector selector) {
		this.send = send;
		this.listen = listen;
		this.selector = selector;
	}

	@Override
	public void run() {
		SelectionKey sKey = null;
		while (true) {
			int nKeys = 0;
			try {
				nKeys = selector.select();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// System.out.println("test");

			if (nKeys > 0) {
				System.out.println("nKeys > 0");
				Set<SelectionKey> keys = selector.selectedKeys();
				for (SelectionKey key : keys) {
					System.out.println("test " + key);
					// 对于发生连接事件
					/*
					 * if(key.isConnectable()) { SocketChannel sc =
					 * (SocketChannel)key.channel();
					 * sc.configureBlocking(false);
					 * //注册感兴趣的IO事件，通常不直接注册写事件，在发送缓冲区未满的情况下
					 * //一直是可写的，所以如果注册了写事件，而又不写数据，则很容易造成CPU消耗100% sKey =
					 * sc.register(selector, SelectionKey.OP_READ);
					 * 
					 * //完成连接的建立 sc.finishConnect(); } else
					 */
					System.out.println(key.isConnectable() + " " + key.isWritable() + " " + key.isReadable() + " "
							+ key.isAcceptable());
					if (key.isConnectable()) {
						try {
							System.out.println("key.isConnectable()");
							send.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
						} catch (ClosedChannelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (key.isReadable()) {
						// 有流可读取

						System.out.println("test 2");
						ByteBuffer buffer = ByteBuffer.allocate(1024);
						//SelectableChannel sc 
						DatagramChannel sc = (DatagramChannel) key.channel();
						//SocketChannel sc = (SocketChannel) key.channel();
						int readBytes = 0;

						try {
							int ret = 0;
							try {
								// 读取目前可读的流，sc.read(buffer)返回的是成功复制到bytebuffer中
								// 的字节数，为阻塞操作，值可能为0，若到流结尾，返回-1
								while ((ret = sc.read(buffer)) > 0) {
									readBytes += ret;
								}
								System.out.println(buffer.array().length);
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
					/*
					 * else if(key.isWritable())//可写入流 { //取消对OP_WRITE事件的注册
					 * ByteBuffer buffer = ByteBuffer.allocate(1024);
					 * key.interestOps(key.interestOps() &
					 * (~SelectionKey.OP_WRITE)); SocketChannel sc =
					 * (SocketChannel)key.channel();
					 * 
					 * //此步为阻塞操作，知道写入操作系统发送缓冲区或者网络IO出现异常
					 * //返回的为成功写入的字节数，若缓冲区已满，返回0 int writeenedSize =
					 * sc.write(buffer);
					 * 
					 * //若未写入，继续注册感兴趣的OP_WRITE事件 if(writeenedSize == 0) {
					 * key.interestOps(key.interestOps() |
					 * SelectionKey.OP_WRITE); } }
					 */
				}

			}

			/*
			 * byte[] listen = new byte[100]; DatagramPacket listenPacket = new
			 * DatagramPacket(listen, listen.length);
			 * 
			 * try { socket.receive(listenPacket);
			 * 
			 * } catch (IOException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 */
		}
	}

	public void send(ByteBuffer buffer) {
		// System.out.println("test send 1");
		// int nKeys = 0;
		buffer.flip();
		try {
			int writeenedSize = send.write(buffer);
			/*
			 * nKeys = selector.select(); if (nKeys > 0) { Set<SelectionKey>
			 * keys = selector.selectedKeys(); System.out.println("test send 12"
			 * ); for (SelectionKey key : keys) { if (key.isWritable()) {
			 * 
			 * } } }
			 */

			/*
			 * if (packet != null) {
			 * 
			 * System.out.println("send"); ByteBuffer buffer =
			 * ByteBuffer.allocate(2); int writeenedSize = send.write(buffer); }
			 */
			// listen.register(selector, SelectionKey.OP_WRITE);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
