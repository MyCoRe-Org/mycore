/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;

import org.mycore.common.MCRCoreVersion;
import org.mycore.common.config.MCRConfiguration2;

public class MCRHttpUtils {
    private static final Set<String> HOP_BY_HOP_HEADERS;
    static {
        Set<String> hopHeaders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        hopHeaders.addAll(Set.of(
            "Connection", "Proxy-Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization",
            "TE", "Trailers", "Transfer-Encoding", "Upgrade"));
        HOP_BY_HOP_HEADERS = Collections.unmodifiableSet(hopHeaders);
    }

    public static HttpClient getHttpClient() {
        //setup http client
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }

    public static String getHttpUserAgent() {
        return String.format(Locale.ROOT, "MyCoRe/%s (%s; java %s)", MCRCoreVersion.getCompleteVersion(),
            MCRConfiguration2.getString("MCR.NameOfProject").orElse("undefined"),
            System.getProperty("java.version"));
    }

    public static HttpRequest.Builder getRequestBuilder() {
        return HttpRequest.newBuilder()
            .header("User-Agent", getHttpUserAgent());
    }

    // Utility method to map status codes to reason phrases
    public static Optional<String> getReasonPhrase(int statusCode) {
        return MCRHttpStatus.resolveStatusCode(statusCode).map(MCRHttpStatus::getReasonPhrase);
    }

    /**
     * Filters out hop-by-hop HTTP headers from the provided {@link HttpHeaders} object.
     * <p>
     * Hop-by-hop headers are specific to a single transport-level connection and must not be forwarded
     * by intermediaries such as proxies or gateways. This method removes these headers, returning a new
     * {@link HttpHeaders} instance without them.
     * <p>
     * The hop-by-hop headers are defined in the HTTP specification and include:
     * <ul>
     *     <li>Connection</li>
     *     <li>Keep-Alive</li>
     *     <li>Proxy-Authenticate</li>
     *     <li>Proxy-Authorization</li>
     *     <li>TE</li>
     *     <li>Trailer</li>
     *     <li>Transfer-Encoding</li>
     *     <li>Upgrade</li>
     * </ul>
     *
     * @param headers The original {@link HttpHeaders} object containing the full set of headers.
     * @return A new {@link HttpHeaders} instance with all hop-by-hop headers removed.
     * @see <a href="https://www.rfc-editor.org/rfc/rfc9110.html#field.connection">RFC 9110: Connection Header Field</a>
     */
    public static HttpHeaders filterHopByHop(HttpHeaders headers) {
        return HttpHeaders.of(headers.map(), (name, value) -> !HOP_BY_HOP_HEADERS.contains(name));
    }
}
