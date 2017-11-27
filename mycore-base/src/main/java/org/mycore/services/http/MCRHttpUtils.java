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

package org.mycore.services.http;

import java.net.URI;
import java.nio.charset.Charset;
import java.text.MessageFormat;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.mycore.common.MCRCoreVersion;
import org.mycore.common.config.MCRConfiguration;

public class MCRHttpUtils {

    public static CloseableHttpClient getHttpClient(HttpClientConnectionManager connectionManager, int maxConnections) {

        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30000).setSocketTimeout(30000).build();

        ConnectionConfig connectionConfig = ConnectionConfig.custom().setCharset(Charset.forName("UTF-8")).build();
        SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).setSoKeepAlive(true)
            .setSoReuseAddress(true).build();

        String userAgent = MessageFormat
            .format("MyCoRe/{0} ({1}; java {2})", MCRCoreVersion.getCompleteVersion(), MCRConfiguration.instance()
                .getString("MCR.NameOfProject", "undefined"), System.getProperty("java.version"));
        //setup http client
        return HttpClients.custom().setConnectionManager(connectionManager)
            .setUserAgent(userAgent).setRetryHandler(new MCRRetryHandler(maxConnections))
            .setDefaultRequestConfig(requestConfig).setDefaultConnectionConfig(connectionConfig)
            .setDefaultSocketConfig(socketConfig).build();
    }

    public static PoolingHttpClientConnectionManager getConnectionManager(int maxConnections) {
        //configure connection manager
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(maxConnections);
        connectionManager.setMaxTotal(maxConnections);
        connectionManager.setValidateAfterInactivity(30000);
        return connectionManager;
    }

    public static HttpHost getHttpHost(String serverUrl) {
        HttpHost host = null;
        //determine host name
        HttpGet serverGet = new HttpGet(serverUrl);
        final URI requestURI = serverGet.getURI();
        if (requestURI.isAbsolute()) {
            host = URIUtils.extractHost(requestURI);
        }
        return host;
    }
}
