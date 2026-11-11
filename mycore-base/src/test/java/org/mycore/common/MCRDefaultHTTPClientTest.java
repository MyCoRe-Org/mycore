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

package org.mycore.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.test.MyCoReTest;

import com.sun.net.httpserver.HttpServer;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.HTTPClient.Class", classNameOf = MCRDefaultHTTPClient.class),
    @MCRTestProperty(key = "MCR.HTTPClient.RequestTimeout", string = "500"),
    @MCRTestProperty(key = "MCR.HTTPClient.MaxCacheEntries", string = "10"),
    @MCRTestProperty(key = "MCR.HTTPClient.MaxObjectSize", string = "1024")
})
public class MCRDefaultHTTPClientTest {

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void configuredRequestTimeoutIsApplied() throws Exception {
        // the server accepts connections on TCP level, but never sends a response
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            URI uri = URI.create("http://" + server.getInetAddress().getHostAddress() + ":"
                + server.getLocalPort() + "/");
            MCRDefaultHTTPClient client = getClient();
            try {
                assertThrows(SocketTimeoutException.class, () -> client.get(uri));
            } finally {
                client.close();
            }
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void configuredCacheIsApplied() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        byte[] body = "<test/>".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            exchange.getResponseHeaders().add("Content-Type", "application/xml");
            exchange.getResponseHeaders().add("Cache-Control", "public, max-age=60");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            URI uri = URI.create("http://" + server.getAddress().getAddress().getHostAddress() + ":"
                + server.getAddress().getPort() + "/");
            MCRDefaultHTTPClient client = getClient();
            try {
                client.get(uri);
                client.get(uri);
            } finally {
                client.close();
            }
            assertEquals(1, requestCount.get(), "second request should be served from cache");
        } finally {
            server.stop(0);
        }
    }

    private static MCRDefaultHTTPClient getClient() {
        return MCRConfiguration2.getInstanceOf(MCRDefaultHTTPClient.class, "MCR.HTTPClient.Class").orElseThrow();
    }

}
