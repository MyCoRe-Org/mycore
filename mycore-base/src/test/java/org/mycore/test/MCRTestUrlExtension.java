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

package org.mycore.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mycore.common.MCRTestUrl;
import org.mycore.common.MCRTestUrlConfiguration;

/**
 * JUnit 5 extension for testing with test URLs in the MyCoRe framework.
 * <p>
 * This extension resolves parameters of type {@link URLStreamHandlerFactory} and {@link URLFactory}
 * that can be used to create working {@link URL} instances for various protocols. Such URLs can be created
 * using {@link URL#of(URI, URLStreamHandler)} (with a URL stream handler returned from a resolved URL stream
 * handler factory as the second parameter) or using {@link URLFactory#createUrl(String)}).
 * <p>
 * Supported protocols can be configured using {@link MCRTestUrlConfiguration#protocols()}. Additional configuration
 * annotation on subclasses and methods add to the set of supported protocols.
 * <p>
 * If connected to, URLs created in the above-mentioned way can be read from. By default, empty content and
 * no headers are returned. Content and headers to be returned from such connections can be configured using
 * {@link MCRTestUrlConfiguration#urls()}. Additional configuration annotations on subclasses and methods add
 * to the set of configured URLs, but replace existing configurations for specific URLs.
 * <p>
 * If connected to, URLs created in the above-mentioned way can not be written to.
 */
public class MCRTestUrlExtension implements Extension, BeforeAllCallback, BeforeEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(MCRTestUrlExtension.class);

    private static final String DATA_KEY = "TEST_URL_EXTENSION_DATA";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        context.getStore(NAMESPACE).put(DATA_KEY, getClassData(context, context.getRequiredTestClass()));
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        context.getStore(NAMESPACE).put(DATA_KEY, getMethodData(context));
    }

    private static Data getMethodData(ExtensionContext context) {

        Data data = getClassData(context, context.getRequiredTestClass());

        Method testMethod = context.getRequiredTestMethod();
        MCRTestUrlConfiguration configuration = testMethod.getAnnotation(MCRTestUrlConfiguration.class);
        if (configuration != null) {
            data = data.extendWith(configuration);
        }

        return data;

    }

    private static Data getClassData(ExtensionContext context, Class<?> testClass) {
        return (Data) context.getRoot().getStore(NAMESPACE).computeIfAbsent(testClass, unprocessedTestClass -> {

            Data data = Data.DEFAULT;

            Class<?> superClass = testClass.getSuperclass();
            if (superClass != null) {
                data = getClassData(context, superClass);
            }

            MCRTestUrlConfiguration configuration = testClass.getAnnotation(MCRTestUrlConfiguration.class);
            if (configuration != null) {
                data = data.extendWith(configuration);
            }

            return data;

        });
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> parameterClass = parameterContext.getParameter().getType();
        return parameterClass == URLStreamHandlerFactory.class || parameterClass == URLFactory.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Data data = (Data) extensionContext.getStore(NAMESPACE).get(DATA_KEY);
        return switch (parameterContext.getParameter().getType()) {
            case Class<?> c when c == URLStreamHandlerFactory.class -> new TestUrlStreamHandlerFactory(data);
            case Class<?> c when c == URLFactory.class -> new URLFactory(data);
            default -> null;
        };
    }

    private record Data(Set<String> protocols, Map<String, UrlData> urls) {

        private static final Data DEFAULT = new Data(
            Collections.singleton(MCRTestUrlConfiguration.DEFAULT_PROTOCOL),
            Collections.emptyMap());

        private Data extendWith(MCRTestUrlConfiguration configuration) {
            return new Data(mergeProtocols(configuration), mergeUrls(configuration));
        }

        private Set<String> mergeProtocols(MCRTestUrlConfiguration configuration) {
            Set<String> protocols = new HashSet<>(this.protocols);
            protocols.addAll(Arrays.asList(configuration.protocols()));
            return protocols;
        }

        private Map<String, UrlData> mergeUrls(MCRTestUrlConfiguration configuration) {
            Map<String, UrlData> headers = new HashMap<>(this.urls);
            Arrays.stream(configuration.urls()).forEach(
                url -> headers.put(url.url(), new UrlData(url.contentEncoding().decode(url), toHeaders(url))));
            return headers;
        }

        private SequencedMap<String, List<String>> toHeaders(MCRTestUrl url) {
            SequencedMap<String, List<String>> headers = new LinkedHashMap<>();
            Arrays.stream(url.headers()).forEach(
                header -> headers.computeIfAbsent(header.name(), key -> new ArrayList<>()).add(header.value()));
            return headers;
        }

    }

    private record UrlData(byte[] content, SequencedMap<String, List<String>> headers) {
    }

    public static final class URLFactory {

        private final Data data;

        private URLFactory(Data data) {
            this.data = data;
        }

        public URL createUrl(String urlString) throws MalformedURLException {

            URI uri = URI.create(urlString);
            String protocol = uri.getScheme();

            if (!data.protocols.contains(protocol)) {
                throw new MalformedURLException("unknown protocol: " + protocol);
            }

            return URL.of(uri, new TestUrlStreamHandler(protocol, data));

        }

        public URLStreamHandlerFactory toUrlStreamHandlerFactory() {
            return new TestUrlStreamHandlerFactory(data);
        }

    }

    private static class TestUrlStreamHandlerFactory implements URLStreamHandlerFactory {

        private final Data data;

        private TestUrlStreamHandlerFactory(Data data) {
            this.data = data;
        }

        @Override
        public URLStreamHandler createURLStreamHandler(String protocol) {
            if (data.protocols().contains(protocol)) {
                return new TestUrlStreamHandler(protocol, data);
            }
            return null;
        }

    }

    private static class TestUrlStreamHandler extends URLStreamHandler {

        private final String protocol;

        private final Data data;

        private TestUrlStreamHandler(String protocol, Data data) {
            this.protocol = protocol;
            this.data = data;
        }

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            if (!Objects.equals(protocol, url.getProtocol())) {
                throw new IOException("protocol is not " + protocol + ": " + url.getProtocol());
            }
            return new TestUrlConnection(url, data.urls.get(url.toString()));
        }
    }

    private static class TestUrlConnection extends URLConnection {

        private final UrlData data;

        private final List<String> headerNames;

        private final Map<String, List<String>> headers;

        private TestUrlConnection(URL url, UrlData data) {
            super(url);
            this.data = data;
            this.headerNames = List.copyOf(data.headers.keySet());
            this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            this.headers.putAll(data.headers);
        }

        @Override
        public void connect() {
            if (!connected) {
                connected = true;
            }
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(data.content);
        }

        @Override
        public int getContentLength() {
            return data.content.length;
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return Collections.unmodifiableMap(data.headers);
        }

        @Override
        public String getHeaderFieldKey(int n) {
            if (n >= headerNames.size()) {
                return null;
            }
            return headerNames.get(n);
        }

        @Override
        public String getHeaderField(int n) {
            if (n >= headerNames.size()) {
                return null;
            }
            return getHeaderField(headerNames.get(n));
        }

        @Override
        public String getHeaderField(String name) {
            List<String> values = headers.get(name);
            if (values == null || values.isEmpty()) {
                return null;
            }
            return values.getLast();
        }

    }

}
