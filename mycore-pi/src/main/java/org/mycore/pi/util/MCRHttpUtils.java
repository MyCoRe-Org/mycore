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

package org.mycore.pi.util;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;

public class MCRHttpUtils {
    public static HttpClientBuilder getHttpClient() {
        int timeout = 5;

        return HttpClientBuilder.create()
            .setUserAgent(org.mycore.services.http.MCRHttpUtils.getHttpUserAgent())
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setConnectionRequestTimeout(timeout, TimeUnit.SECONDS)
                    .build())
            .setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setDefaultConnectionConfig(
                        ConnectionConfig.custom()
                            .setConnectTimeout(timeout, TimeUnit.SECONDS)
                            .setSocketTimeout(timeout, TimeUnit.SECONDS)
                            .build())
                    .setTlsSocketStrategy(DefaultClientTlsStrategy.createSystemDefault())
                    .build());
    }

    public static HttpClientResponseHandler<MCRContent> getMCRContentResponseHandler(URI requestURI) {
        return response -> {
            if (response.getCode() == HttpStatus.SC_OK) {
                return new MCRStreamContent(response.getEntity().getContent(), requestURI.toString()).getReusableCopy();
            } else {
                throw new MCRException("HTTP Response Code: " + response.getCode());
            }
        };
    }
}
