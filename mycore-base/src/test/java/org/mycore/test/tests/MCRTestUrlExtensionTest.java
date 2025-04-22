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

package org.mycore.test.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestUrl;
import org.mycore.common.MCRTestUrlConfiguration;
import org.mycore.test.MCRTestUrlExtension;
import org.mycore.test.MCRTestUrlExtension.URLFactory;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRTestUrlExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MCRTestUrlExtensionTest {

    private static URL createUrl(URLStreamHandlerFactory factory, String url) throws MalformedURLException {
        return URL.of(URI.create(url), factory.createURLStreamHandler(url.substring(0, url.indexOf(':'))));
    }

    @BeforeAll
    public static void testProtocolAvailableInBeforeAll(URLStreamHandlerFactory factory) {
        assertDoesNotThrow(() -> createUrl(factory, "test://foo"));
    }

    @BeforeAll
    public static void testProtocolAvailableInBeforeAll(URLFactory factory) {
        assertDoesNotThrow(() -> factory.createUrl("test://foo"));
    }

    @BeforeEach
    public void testProtocolAvailableInBeforeEach(URLStreamHandlerFactory factory) {
        assertDoesNotThrow(() -> createUrl(factory, "test://foo"));
    }

    @BeforeEach
    public void testProtocolAvailableInBeforeEach(URLFactory factory) {
        assertDoesNotThrow(() -> factory.createUrl("test://foo"));
    }

    @Test
    @Order(10)
    public void testProtocolAvailable(URLStreamHandlerFactory factory) {
        assertDoesNotThrow(() -> createUrl(factory, "test://foo"));
    }

    @Test
    @Order(11)
    public void testProtocolAvailable(URLFactory factory) {
        assertDoesNotThrow(() -> factory.createUrl("test://foo"));
    }

    @Test
    @Order(20)
    public void test2ProtocolNotAvailable(URLStreamHandlerFactory factory) {
        assertThrows(MalformedURLException.class, () -> createUrl(factory, "test2://foo"));
    }

    @Test
    @Order(21)
    public void test2ProtocolNotAvailable(URLFactory factory) {
        assertThrows(MalformedURLException.class, () -> factory.createUrl("test2://foo"));
    }

    @Test
    @Order(30)
    @MCRTestUrlConfiguration(protocols = "test2")
    public void test2ProtocolAvailable(URLStreamHandlerFactory factory) {
        assertDoesNotThrow(() -> createUrl(factory, "test2://foo"));
    }

    @Test
    @Order(31)
    @MCRTestUrlConfiguration(protocols = "test2")
    public void test2ProtocolAvailable(URLFactory factory) {
        assertDoesNotThrow(() -> factory.createUrl("test2://foo"));
    }

    @Test
    @Order(40)
    @SuppressWarnings("resource")
    @MCRTestUrlConfiguration(urls = {
        @MCRTestUrl(url = "test://lorem", content = "Lorem Ipsum",
            contentEncoding = MCRTestUrl.ContentEncoding.UTF8_STRING)
    })
    public void urlWithStringContent(URLFactory factory) throws IOException {

        byte[] bytes = factory.createUrl("test://lorem").openStream().readAllBytes();
        assertEquals("Lorem Ipsum", new String(bytes, StandardCharsets.UTF_8));

    }

    @Test
    @Order(41)
    @SuppressWarnings("resource")
    @MCRTestUrlConfiguration(urls = {
        @MCRTestUrl(url = "test://pi", content = "AwEEAQUJAgYFAwU=",
            contentEncoding = MCRTestUrl.ContentEncoding.BASE64_BYTES)
    })
    public void urlWithBinaryContent(URLFactory factory) throws IOException {
        byte[] bytes = factory.createUrl("test://pi").openStream().readAllBytes();
        assertArrayEquals(new byte[] { 3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5 }, bytes);
    }

    @Test
    @Order(42)
    @MCRTestUrlConfiguration(urls = {
        @MCRTestUrl(url = "test://headers", headers = {
            @MCRTestUrl.Header(name = "Header1", value = "Value1"),
            @MCRTestUrl.Header(name = "Header2", value = "Value2")
        })
    })
    public void urlWithHeaders(URLFactory factory) throws IOException {

        URLConnection urlConnection = factory.createUrl("test://headers").openConnection();

        assertEquals("Header1", urlConnection.getHeaderFieldKey(0));
        assertEquals("Value1", urlConnection.getHeaderField(0));
        assertEquals("Value1", urlConnection.getHeaderField("Header1"));
        assertEquals("Value1", urlConnection.getHeaderField("header1"));
        assertEquals("Header2", urlConnection.getHeaderFieldKey(1));
        assertEquals("Value2", urlConnection.getHeaderField(1));
        assertEquals("Value2", urlConnection.getHeaderField("Header2"));
        assertEquals("Value2", urlConnection.getHeaderField("header2"));

        assertNull(urlConnection.getHeaderFieldKey(2));
        assertNull(urlConnection.getHeaderField(2));
        assertNull(urlConnection.getHeaderField("Header3"));
        assertNull(urlConnection.getHeaderField("header3"));

        Map<String, List<String>> headers = Map.of(
            "Header1",
            List.of("Value1"),
            "Header2",
            List.of("Value2"));
        assertEquals(headers, urlConnection.getHeaderFields());

    }

    @Test
    @Order(43)
    @MCRTestUrlConfiguration(urls = {
        @MCRTestUrl(url = "test://headers", headers = {
            @MCRTestUrl.Header(name = "Header1", value = "Value1"),
            @MCRTestUrl.Header(name = "Header2", value = "Value2"),
            @MCRTestUrl.Header(name = "Header1", value = "Value1Update"),
            @MCRTestUrl.Header(name = "Header2", value = "Value2Update")
        })
    })
    public void urlWithRepeatedHeaders(URLFactory factory) throws IOException {

        URLConnection urlConnection = factory.createUrl("test://headers").openConnection();

        assertEquals("Header1", urlConnection.getHeaderFieldKey(0));
        assertEquals("Value1Update", urlConnection.getHeaderField(0));
        assertEquals("Value1Update", urlConnection.getHeaderField("Header1"));
        assertEquals("Header2", urlConnection.getHeaderFieldKey(1));
        assertEquals("Value2Update", urlConnection.getHeaderField(1));
        assertEquals("Value2Update", urlConnection.getHeaderField("Header2"));

        Map<String, List<String>> headers = Map.of(
            "Header1",
            List.of("Value1", "Value1Update"),
            "Header2",
            List.of("Value2", "Value2Update"));
        assertEquals(headers, urlConnection.getHeaderFields());

    }

    @Nested
    @MCRTestUrlConfiguration(protocols = "test2")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class TestClassWithAdditionalProtocol {

        @Test
        @Order(10)
        public void test2ProtocolAvailable(URLStreamHandlerFactory factory) {
            assertDoesNotThrow(() -> createUrl(factory, "test://foo"));
        }

        @Test
        @Order(11)
        public void test2ProtocolAvailable(MCRTestUrlExtension.URLFactory factory) {
            assertDoesNotThrow(() -> factory.createUrl("test://foo"));
        }

    }

    @Nested
    @MCRTestUrlConfiguration(urls = {
        @MCRTestUrl(url = "test://foo", content = "class-foo"),
        @MCRTestUrl(url = "test://bar", content = "class-bar")
    })
    class TestClassWithUrls {

        @Test
        @SuppressWarnings("resource")
        @MCRTestUrlConfiguration(urls = {
            @MCRTestUrl(url = "test://bar", content = "method-bar"),
            @MCRTestUrl(url = "test://baz", content = "method-baz")
        })
        public void methodWithDirectUrlOverwrite(URLFactory factory) throws IOException {

            byte[] fooBytes = factory.createUrl("test://foo").openStream().readAllBytes();
            byte[] barBytes = factory.createUrl("test://bar").openStream().readAllBytes();
            byte[] bazBytes = factory.createUrl("test://baz").openStream().readAllBytes();

            assertEquals("class-foo", new String(fooBytes, StandardCharsets.UTF_8));
            assertEquals("method-bar", new String(barBytes, StandardCharsets.UTF_8));
            assertEquals("method-baz", new String(bazBytes, StandardCharsets.UTF_8));

        }

        @Test
        @SuppressWarnings("resource")
        public void methodWithIndirectUrlOverwrite(URLFactory factory) throws IOException {

            byte[] barBytes = factory.createUrl("test://bar").openStream().readAllBytes();

            assertEquals(getBarValue(), new String(barBytes, StandardCharsets.UTF_8));

        }

        protected String getBarValue() {
            return "class-bar";
        }

    }

    @Nested
    @MCRTestUrlConfiguration(urls = {
        @MCRTestUrl(url = "test://bar", content = "subclass-bar"),
    })
    class TestClassWithUrlOverwrite extends TestClassWithUrls {

        protected String getBarValue() {
            return "subclass-bar";
        }

    }

}
