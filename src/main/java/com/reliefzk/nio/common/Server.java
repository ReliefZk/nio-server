package com.reliefzk.nio.common;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 服务
 * @author ReliefZk
 */

public class Server implements Runnable {
	private static List<SelectionKey> wpool = new LinkedList<SelectionKey>();
	private static Selector selector;
	private ServerSocketChannel sschannel;
	protected Notifier notifier = Notifier.getNotifier();

	private static int MAX_THREADS = 4;

	public Server(int port) throws Exception {

		for (int i = 0; i < MAX_THREADS; i++) {
			Reader reader = new Reader();
			Writer writer = new Writer();
			reader.start();
			writer.start();
		}
		
		/* serverchanel初始化 */
		selector = Selector.open();
		sschannel = ServerSocketChannel.open();
		sschannel.configureBlocking(false);
		sschannel.socket().bind(new InetSocketAddress(port));
		sschannel.register(selector, SelectionKey.OP_ACCEPT);
		System.out.println("Server started ...");
		System.out.println("Server listening on port: " + port);
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				int num = 0;
				num = selector.select();

				if (num > 0) {
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> it = selectedKeys.iterator();
					while (it.hasNext()) {
						SelectionKey key = (SelectionKey) it.next();
						it.remove();
						if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
							// Accept the new connection
							ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
							notifier.fireOnAccept();

							SocketChannel sc = ssc.accept();
							sc.configureBlocking(false);

							Request request = new Request(sc);
							notifier.fireOnAccepted(request);

							sc.register(selector, SelectionKey.OP_READ, request);
						} else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
							Reader.processRequest(key); // �ύ�������̶߳�ȡ�ͻ�������
							key.cancel();
						} else if ((key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
							Writer.processRequest(key); // �ύд�����߳���ͻ��˷��ͻ�Ӧ����
							key.cancel();
						}
					}
				} else {
					addRegister();
				}
			} catch (Exception e) {
				notifier.fireOnError("Error occured in Server: " + e.getMessage());
				continue;
			}
		}
	}

	private void addRegister() {
		synchronized (wpool) {
			while (!wpool.isEmpty()) {
				SelectionKey key = (SelectionKey) wpool.remove(0);
				SocketChannel schannel = (SocketChannel) key.channel();
				try {
					schannel.register(selector, SelectionKey.OP_WRITE, key.attachment());
				} catch (Exception e) {
					try {
						schannel.finishConnect();
						schannel.close();
						schannel.socket().close();
						notifier.fireOnClosed((Request) key.attachment());
					} catch (Exception e1) {
					}
					notifier.fireOnError("Error occured in addRegister: " + e.getMessage());
				}
			}
		}
	}

	public static void processWriteRequest(SelectionKey key) {
		synchronized (wpool) {
			wpool.add(wpool.size(), key);
			wpool.notifyAll();
		}
		selector.wakeup();
	}
}
