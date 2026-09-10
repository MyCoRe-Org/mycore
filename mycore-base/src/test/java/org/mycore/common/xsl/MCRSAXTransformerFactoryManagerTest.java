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

package org.mycore.common.xsl;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.test.MyCoReTest;
import org.xml.sax.helpers.AttributesImpl;

@MyCoReTest
public class MCRSAXTransformerFactoryManagerTest {

    private static final String STYLESHEET = """
        <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
          <xsl:template match="/root"><result>ok</result></xsl:template>
        </xsl:stylesheet>
        """;

    private static final String NAMESPACE_PARAMETER_STYLESHEET = """
        <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
            xmlns:test="urn:test">
          <xsl:param name="test:value"/>
          <xsl:template match="/"><result><xsl:value-of select="$test:value"/></result></xsl:template>
        </xsl:stylesheet>
        """;

    @Test
    public void sameProviderUsesSameHolder() {
        MCRSAXTransformerFactoryManager saxon =
            MCRSAXTransformerFactoryManager.obtainInstance("Saxon");
        MCRSAXTransformerFactoryManager sameSaxon =
            MCRSAXTransformerFactoryManager.obtainInstance("Saxon");
        MCRSAXTransformerFactoryManager xalan =
            MCRSAXTransformerFactoryManager.obtainInstance("Xalan");

        assertSame(saxon, sameSaxon);
        assertNotSame(saxon, xalan);
    }

    @Test
    @SuppressWarnings("removal")
    public void prefersExactFactoryClassForLegacyLookup() {
        MCRSAXTransformerFactoryManager slowXalan = MCRSAXTransformerFactoryManager.obtainInstance("SlowXalan");

        assertSame(slowXalan, MCRSAXTransformerFactoryManager
            .obtainInstance(org.apache.xalan.processor.TransformerFactoryImpl.class));
    }

    @Test
    @SuppressWarnings("removal")
    public void sharesUnregisteredLegacyFactoryClass() {
        MCRSAXTransformerFactoryManager first =
            MCRSAXTransformerFactoryManager.obtainInstance(UnregisteredTransformerFactory.class);
        MCRSAXTransformerFactoryManager second =
            MCRSAXTransformerFactoryManager.obtainInstance(UnregisteredTransformerFactory.class);

        assertSame(first, second);
        assertEquals(UnregisteredTransformerFactory.class, first.getFactoryClass());
        assertTrue(first.isAccessSerialized());
    }

    @Test
    public void keepsNamespaceParameterSupportInSlowXalan() throws TransformerConfigurationException {
        Templates slowXalan = compile("SlowXalan", NAMESPACE_PARAMETER_STYLESHEET);
        Templates optimizedXalan = compile("Xalan", NAMESPACE_PARAMETER_STYLESHEET);
        String parameterName = "{urn:test}value";

        assertDoesNotThrow(() -> slowXalan.newTransformer().setParameter(parameterName, "supported"));
        assertThrows(IllegalArgumentException.class,
            () -> optimizedXalan.newTransformer().setParameter(parameterName, "unsupported"));
    }

    @Test
    public void serializesAllOperationsOnFactory() throws Exception {
        Source source = new StreamSource();
        Templates templates = new DummyTemplates();
        CountDownLatch compileEntered = new CountDownLatch(1);
        CountDownLatch releaseCompile = new CountDownLatch(1);
        CountDownLatch handlerTaskStarted = new CountDownLatch(1);
        CountDownLatch handlerCallEntered = new CountDownLatch(1);
        AtomicReference<Thread> handlerThread = new AtomicReference<>();
        BlockingTransformerFactory factory = new BlockingTransformerFactory(templates, compileEntered,
            releaseCompile, handlerCallEntered);

        MCRSAXTransformerFactoryManager sharedFactory =
            new MCRSAXTransformerFactoryManager("Blocking", factory, true, () -> null);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Templates> compile = executor.submit(() -> sharedFactory.newTemplates(source));
            assertTrue(compileEntered.await(5, SECONDS));

            Future<TransformerHandler> createHandler = executor.submit(() -> {
                handlerThread.set(Thread.currentThread());
                handlerTaskStarted.countDown();
                return sharedFactory.newTransformerHandler(templates);
            });
            assertTrue(handlerTaskStarted.await(5, SECONDS));
            assertThreadBlocked(handlerThread.get());
            assertEquals(1, handlerCallEntered.getCount());

            releaseCompile.countDown();
            assertSame(templates, compile.get(5, SECONDS));
            assertNull(createHandler.get(5, SECONDS));
            assertTrue(handlerCallEntered.await(5, SECONDS));
        } finally {
            releaseCompile.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    public void permitsConcurrentOperationsWhenSerializationIsDisabled() throws Exception {
        Source source = new StreamSource();
        Templates templates = new DummyTemplates();
        CountDownLatch compileEntered = new CountDownLatch(1);
        CountDownLatch releaseCompile = new CountDownLatch(1);
        CountDownLatch handlerCallEntered = new CountDownLatch(1);
        BlockingTransformerFactory factory = new BlockingTransformerFactory(templates, compileEntered,
            releaseCompile, handlerCallEntered);

        MCRSAXTransformerFactoryManager sharedFactory =
            new MCRSAXTransformerFactoryManager("Concurrent", factory, false, () -> null);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Templates> compile = executor.submit(() -> sharedFactory.newTemplates(source));
            assertTrue(compileEntered.await(5, SECONDS));

            Future<TransformerHandler> createHandler =
                executor.submit(() -> sharedFactory.newTransformerHandler(templates));
            assertTrue(handlerCallEntered.await(5, SECONDS));

            releaseCompile.countDown();
            assertSame(templates, compile.get(5, SECONDS));
            assertNull(createHandler.get(5, SECONDS));
        } finally {
            releaseCompile.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    public void transformsConcurrentlyWithSaxonAndXalan() throws Exception {
        assertConcurrentTransformations("Saxon");
        assertConcurrentTransformations("Xalan");
    }

    @Test
    public void reportsRecursiveInitialization() {
        AtomicReference<MCRSAXTransformerFactoryManager> manager = new AtomicReference<>();
        manager.set(new MCRSAXTransformerFactoryManager("Recursive", new MCRXalanTransformerFactory(), false,
            () -> {
                try {
                    manager.get().newTransformer();
                } catch (TransformerConfigurationException e) {
                    throw new AssertionError(e);
                }
                return null;
            }));

        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> manager.get().newTransformer());

        assertTrue(exception.getMessage().contains("Recursive initialization"));
        assertTrue(exception.getMessage().contains("Recursive"));
    }

    private static void assertThreadBlocked(Thread thread) {
        long deadline = System.nanoTime() + SECONDS.toNanos(5);
        while (thread.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertEquals(Thread.State.BLOCKED, thread.getState());
    }

    private void assertConcurrentTransformations(String factoryId) throws Exception {
        MCRSAXTransformerFactoryManager sharedFactory = MCRSAXTransformerFactoryManager.obtainInstance(factoryId);
        Templates templates = sharedFactory.newTemplates(new StreamSource(new StringReader(STYLESHEET)));
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                results.add(executor.submit(() -> transform(sharedFactory, templates)));
            }
            for (Future<String> result : results) {
                assertTrue(result.get(5, SECONDS).contains("<result>ok</result>"));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private Templates compile(String factoryId, String stylesheet) throws TransformerConfigurationException {
        return MCRSAXTransformerFactoryManager.obtainInstance(factoryId)
            .newTemplates(new StreamSource(new StringReader(stylesheet)));
    }

    private String transform(MCRSAXTransformerFactoryManager sharedFactory, Templates templates) throws Exception {
        TransformerHandler handler = sharedFactory.newTransformerHandler(templates);
        StringWriter result = new StringWriter();
        handler.setResult(new StreamResult(result));
        handler.startDocument();
        handler.startElement("", "root", "root", new AttributesImpl());
        handler.endElement("", "root", "root");
        handler.endDocument();
        return result.toString();
    }

    private static final class BlockingTransformerFactory extends MCRXalanTransformerFactory {

        private final Templates templates;

        private final CountDownLatch compileEntered;

        private final CountDownLatch releaseCompile;

        private final CountDownLatch handlerCallEntered;

        private BlockingTransformerFactory(Templates templates, CountDownLatch compileEntered,
            CountDownLatch releaseCompile, CountDownLatch handlerCallEntered) {
            this.templates = templates;
            this.compileEntered = compileEntered;
            this.releaseCompile = releaseCompile;
            this.handlerCallEntered = handlerCallEntered;
        }

        @Override
        public Templates newTemplates(Source source) throws TransformerConfigurationException {
            compileEntered.countDown();
            try {
                if (!releaseCompile.await(5, SECONDS)) {
                    throw new TransformerConfigurationException("Timed out waiting to release compilation");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TransformerConfigurationException(e);
            }
            return templates;
        }

        @Override
        public TransformerHandler newTransformerHandler(Templates templates) {
            handlerCallEntered.countDown();
            return null;
        }

    }

    private static final class DummyTemplates implements Templates {

        @Override
        public Transformer newTransformer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Properties getOutputProperties() {
            return new Properties();
        }

    }

    public static final class UnregisteredTransformerFactory extends MCRXalanTransformerFactory {
    }

}
