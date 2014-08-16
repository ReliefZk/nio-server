package com.reliefzk.nio.common;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

/**
 * @author ReliefZk
 */

public final class Writer extends Thread {
    private static List<SelectionKey> pool = new LinkedList<SelectionKey>();
    private static Notifier notifier = Notifier.getNotifier();

    public Writer() {
    }

    /**
     */
    public void run() {
        while (true) {
            try {
                SelectionKey key;
                synchronized (pool) {
                    while (pool.isEmpty()) {
                        pool.wait();
                    }
                    key = (SelectionKey) pool.remove(0);
                }

                write(key);
            }
            catch (Exception e) {
                continue;
            }
        }
    }

    /**
     * @param key SelectionKey
     */
    public void write(SelectionKey key) {
        try {
            SocketChannel sc = (SocketChannel) key.channel();
            Response response = new Response(sc);

            notifier.fireOnWrite((Request)key.attachment(), response);

            sc.finishConnect();
            sc.socket().close();
            sc.close();

            notifier.fireOnClosed((Request)key.attachment());
        }
        catch (Exception e) {
            notifier.fireOnError("Error occured in Writer: " + e.getMessage());
        }
    }

    /**
     */
    public static void processRequest(SelectionKey key) {
        synchronized (pool) {
            pool.add(pool.size(), key);
            pool.notifyAll();
        }
    }
}
