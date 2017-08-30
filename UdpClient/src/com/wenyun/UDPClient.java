package com.wenyun;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Scanner;
import java.util.Set;

import javax.swing.text.ChangedCharSetException;

public class UDPClient {
	//private static String ip = "127.0.0.1";
	private static String ip = "114.115.143.70";
	private static int port = 10561;
	private static DatagramChannel sendChannel;
	private static Selector selector;
	public static SocketAddress target;
	public static SocketAddress changeTarget;

	public static void main(String[] args) {
		UDPClient client = new UDPClient();
		
		try {
			sendChannel = DatagramChannel.open();
			sendChannel.configureBlocking(false);
			selector = Selector.open();
			target = new InetSocketAddress(ip, port);
			sendChannel.connect(target);
			sendChannel.register(selector, SelectionKey.OP_READ);
		} catch (Exception e) {
			e.printStackTrace();
		}

		/*
		 * if (socket == null) { System.out.println("create socket error");
		 * System.exit(0); }
		 */

		LoopThread loopThread = new LoopThread(sendChannel, selector, client);
		loopThread.start();

		Scanner scanner = new Scanner(System.in);
		while (true) {
			String str = scanner.nextLine();
			System.out.println("System.in " + str);
			if ("1".equals(str)) {
				try {
					if (!ip.equals(((InetSocketAddress) sendChannel.getRemoteAddress()).getAddress())) {
						sendChannel.disconnect();
						sendChannel.connect(target);
						sendChannel.register(selector, SelectionKey.OP_READ);
					}
				} catch (Exception e) {
					// TODO: handle exception
				}

				byte[] test = new byte[1];
				test[0] = 0x01;

				ByteBuffer b = ByteBuffer.allocate(1);
				b.put(test);
				loopThread.send(b);

			} else if ("2".equals(str)) {
				byte[] test = new byte[2];
				test[0] = 0x02;
				test[1] = 0x06;
				ByteBuffer b = ByteBuffer.allocate(2);
				b.put(test);

				loopThread.send(b);
				try {
					System.out.println(sendChannel.getRemoteAddress());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else if ("3".equals(str)) {
				String sendHost = scanner.nextLine();
				String[] host = sendHost.split(":");
				changeTarget = new InetSocketAddress(host[0], Integer.parseInt(host[1]));
				
			} else if ("4".equals(str)) {
				for(int i=0;i<100; i++) {
					byte[] test = new byte[2];
					test[0] = 0x03;
					test[1] = 0x06;
					ByteBuffer b = ByteBuffer.allocate(2);
					b.put(test);

					loopThread.send(b);
				}
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
	
	public static void changeClannel() {
		try {
			sendChannel.disconnect();
			sendChannel.connect(changeTarget);
			sendChannel.register(selector, SelectionKey.OP_READ);
			System.out.println(sendChannel.getRemoteAddress());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class LoopThread extends Thread {
	DatagramChannel send;
	Selector selector;
	UDPClient client;

	public LoopThread(DatagramChannel send, Selector selector, UDPClient client) {
		this.send = send;
		this.selector = selector;
		this.client = client;
	}

	@Override
	public void run() {
		ByteBuffer buffer = ByteBuffer.allocate(50);
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
						System.out.println("key.isReadable()");
						
						DatagramChannel sc = (DatagramChannel) key.channel();
						int readBytes = 0;

						try {
							int ret = 0;
							try {
								while ((ret = sc.read(buffer)) > 0) {
									System.out.println("while ret = " + ret);
									readBytes += ret;
								}
								
								buffer.flip();
								
								if (buffer.array()[0] == buffer.array()[1] && buffer.array()[0] == 0x01) {
									client.changeClannel();
									for(int i=0;i<10; i++) {
										System.out.println(i);
										byte[] test = new byte[2];
										test[0] = 0x02;
										test[1] = 0x06;
										ByteBuffer b = ByteBuffer.allocate(2);
										b.put(test);
										send(b);
										Thread.sleep(1000);
									}
									client.changeTarget = client.target;
									client.changeClannel();
									
									byte[] test = "ok".getBytes();
									ByteBuffer b = ByteBuffer.allocate(2);
									b.put(test);
									send(b);
								}
								
								System.out.println("buffer length :" + buffer.array().length + " " + new String(buffer.array()));
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
