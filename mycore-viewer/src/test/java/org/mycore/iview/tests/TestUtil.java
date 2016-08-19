package org.mycore.iview.tests;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class TestUtil {
    public static <T> T instantiate(final String className, final Class<T> type) {
        try {
            return type.cast(Class.forName(className).newInstance());
        } catch (final InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public static InetAddress getLocalAdress(String remoteHost, int port) throws IOException {
        InetSocketAddress socketAddr = new InetSocketAddress(remoteHost, port);
        SocketChannel socketChannel = SocketChannel.open(socketAddr);
        InetSocketAddress localAddress = (InetSocketAddress) socketChannel.getLocalAddress();
        return localAddress.getAddress();
    }
}
