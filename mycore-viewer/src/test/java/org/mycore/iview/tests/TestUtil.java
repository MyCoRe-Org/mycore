/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.iview.tests;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class TestUtil {
    public static <T> T instantiate(final String className, final Class<T> type) {
        try {
            return type.cast(Class.forName(className).getDeclaredConstructor().newInstance());
        } catch (final ReflectiveOperationException e) {
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
