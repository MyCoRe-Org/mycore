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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRErrorListenerTest {

    private static final String STYLESHEET = "xslt-message/xsl-message.xsl";

    private static final String MESSAGE = "Hello from xsl:message";

    private static final String TERMINATE_MESSAGE = "No host present!";

    private MessageAppender appender;

    @BeforeEach
    public void addAppender() {
        appender = new MessageAppender();
        appender.start();
        getListenerLogger().addAppender(appender);
    }

    @AfterEach
    public void removeAppender() {
        getListenerLogger().removeAppender(appender);
        appender.stop();
    }

    @Test
    public void saxonMessageIsLogged() throws IOException {
        MCRXSLTransformer transformer = MCRXSLTransformer.obtainInstance(net.sf.saxon.TransformerFactoryImpl.class,
            STYLESHEET);
        transformer.transform(new MCRJDOMContent(new Element("in"))).asString();
        assertLogged(MESSAGE);
    }

    @Test
    public void saxonTerminateMessage() {
        MCRXSLTransformer transformer = MCRXSLTransformer.obtainInstance(net.sf.saxon.TransformerFactoryImpl.class,
            STYLESHEET);

        MCRException mcrException = assertThrows(MCRException.class,
            () -> transformer.transform(new MCRJDOMContent(new Element("in")), terminate()).asString());
        assertContains(mcrException.getMessage(), TERMINATE_MESSAGE);

        IOException ioException = assertThrows(IOException.class,
            () -> transformer.transform(new MCRJDOMContent(new Element("in")), new ByteArrayOutputStream(),
                terminate()));
        assertContains(ioException.getMessage(), TERMINATE_MESSAGE);

        assertLogged(TERMINATE_MESSAGE);
    }

    @Test
    public void saxonPlainTransformerWithSetTo() throws TransformerException {
        TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
        Transformer transformer = factory.newTransformer(
            new StreamSource(getClass().getClassLoader().getResource(STYLESHEET).toString()));
        MCRErrorListener.setTo(transformer);
        transformer.setParameter("terminate", "yes");

        TransformerException exception = assertThrows(TransformerException.class,
            () -> transformer.transform(new JDOMSource(new Document(new Element("in"))), new JDOMResult()));
        assertContains(MCRErrorListener.getMyMessageAndLocation(exception), TERMINATE_MESSAGE);
        assertLogged(MESSAGE);
        assertLogged(TERMINATE_MESSAGE);
    }

    @Test
    public void saxonEmptyTerminateMessage() throws TransformerException {
        TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
        Transformer transformer = factory.newTransformer(
            new StreamSource(getClass().getClassLoader().getResource(STYLESHEET).toString()));
        MCRErrorListener.setTo(transformer);
        transformer.setParameter("terminate", "empty");

        TransformerException exception = assertThrows(TransformerException.class,
            () -> transformer.transform(new JDOMSource(new Document(new Element("in"))), new JDOMResult()));
        // falls back to the Saxon message
        assertContains(MCRErrorListener.getMyMessageAndLocation(exception), "xsl:message");
    }

    @Test
    public void xalanTerminateMessage() {
        MCRXSLTransformer transformer = MCRXSLTransformer
            .obtainInstance(org.apache.xalan.processor.TransformerFactoryImpl.class, STYLESHEET);
        IOException ioException = assertThrows(IOException.class,
            () -> transformer.transform(new MCRJDOMContent(new Element("in")), new ByteArrayOutputStream(),
                terminate()));
        assertContains(ioException.getMessage(), TERMINATE_MESSAGE);
        assertLogged(MESSAGE);
    }

    private static MCRParameterCollector terminate() {
        MCRParameterCollector parameters = new MCRParameterCollector(false);
        parameters.setParameter("terminate", "yes");
        return parameters;
    }

    private void assertLogged(String message) {
        assertTrue(appender.messages.stream().anyMatch(m -> m.contains(message)),
            "Message '" + message + "' was not logged: " + appender.messages);
    }

    private static void assertContains(String actual, String expected) {
        assertTrue(actual != null && actual.contains(expected),
            "Expected '" + expected + "' in: " + actual);
    }

    private static Logger getListenerLogger() {
        return (Logger) LogManager.getLogger(MCRErrorListener.class);
    }

    private static final class MessageAppender extends AbstractAppender {

        private final List<String> messages = new CopyOnWriteArrayList<>();

        MessageAppender() {
            super("xsl-message", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            messages.add(event.getMessage().getFormattedMessage());
        }
    }

}
