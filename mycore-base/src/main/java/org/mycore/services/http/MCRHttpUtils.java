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

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;

import org.mycore.common.MCRCoreVersion;
import org.mycore.common.config.MCRConfiguration2;

public class MCRHttpUtils {

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
        return MCRHttpStatus.resolve(statusCode).map(MCRHttpStatus::getReasonPhrase);
    }
}
