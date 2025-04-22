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

package org.mycore.resource.common;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.MCRException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHintKey;
import org.mycore.resource.MCRResourcePath;

/**
 * A {@link MCRSyntheticResourceSpec} consists of a {@link #prefix()} and a {@link #path()}.
 * Prefix and path are concatenated to create the resource {@link URL}.
 * <p>
 * In order to create a resource URL, it first creates a {@link URI}, using {@link URI#create(String)}.
 * It then converts the URI into a URL using {@link URL#of(URI, URLStreamHandler)} with the {@link URLStreamHandler}
 * returned for the URIs protocol by the {@link URLStreamHandlerFactory} hinted at by
 * {@link MCRSyntheticResourceSpec#URL_STREAM_HANDLER_FACTORY}; if such a URL stream handler factory is present
 * and if it returns a URL stream handler for the URIs protocol. In any other case, it uses {@link URI#toURL()}.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRSyntheticResourceSpec#PREFIX_KEY} can be used to
 * specify the prefix to be used.
 * <li> The property suffix {@link MCRSyntheticResourceSpec#PATH_KEY} can be used to
 * specify the path to be used.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.locator.MCRSyntheticResourceLocator
 * [...].Prefix=test:
 * [...].Path=/foo
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRSyntheticResourceSpec.Factory.class)
public record MCRSyntheticResourceSpec(String prefix, MCRResourcePath path) {

    public static final MCRHintKey<URLStreamHandlerFactory> URL_STREAM_HANDLER_FACTORY = new MCRHintKey<>(
        URLStreamHandlerFactory.class,
        MCRSyntheticResourceSpec.class,
        "URL_STREAM_HANDLER_FACTORY",
        URLStreamHandlerFactory::toString);

    public static final String PATH_KEY = "Path";

    public static final String PREFIX_KEY = "Prefix";

    public MCRSyntheticResourceSpec {
        Objects.requireNonNull(prefix, "Prefix must not be null");
        Objects.requireNonNull(path, "Path must not be null");
    }

    public URL toUrl(URLStreamHandlerFactory factory) {
        String urlString = prefix + path.asAbsolutePath();
        try {
            URI uri = URI.create(urlString);
            if (factory != null) {
                URLStreamHandler handler = factory.createURLStreamHandler(uri.getScheme());
                if (handler != null) {
                    return URL.of(uri, handler);
                }
            }
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new MCRException("Unable to create URL from entry: " + urlString, e);
        }
    }

    public static class Factory implements Supplier<MCRSyntheticResourceSpec> {

        @MCRProperty(name = PREFIX_KEY)
        public String prefix;

        @MCRProperty(name = PATH_KEY)
        public String path;

        @Override
        public MCRSyntheticResourceSpec get() {
            return new MCRSyntheticResourceSpec(prefix, MCRResourcePath.ofPath(path).orElseThrow());
        }

    }

}
