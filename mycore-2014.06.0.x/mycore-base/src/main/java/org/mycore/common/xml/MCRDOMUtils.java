package org.mycore.common.xml;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;

/**
 * Helper class to get {@link DocumentBuilder} instances from a common pool
 * @author Thomas Scheffler (yagee)
 */
public class MCRDOMUtils implements Closeable {

    DocumentBuilderFactory docBuilderFactory;

    ConcurrentLinkedQueue<DocumentBuilder> builderQueue;

    private MCRDOMUtils() {
        builderQueue = new ConcurrentLinkedQueue<>();
        docBuilderFactory = DocumentBuilderFactory.newInstance(DocumentBuilderFactoryImpl.class.getName(), getClass()
            .getClassLoader());
        docBuilderFactory.setNamespaceAware(true);
        MCRShutdownHandler.getInstance().addCloseable(this);
    }

    public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilder documentBuilder = LazyHolder.INSTANCE.builderQueue.poll();
        return documentBuilder != null ? resetDocumentBuilder(documentBuilder) : createDocumentBuilder();
    }

    public static DocumentBuilder getDocumentBuilderUnchecked() {
        try {
            return getDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new MCRException(e);
        }
    }

    public static void releaseDocumentBuilder(DocumentBuilder documentBuilder) {
        LazyHolder.INSTANCE.builderQueue.add(documentBuilder);
    }

    /**
     * @param documentBuilder
     * @return
     */
    private static DocumentBuilder resetDocumentBuilder(DocumentBuilder documentBuilder) {
        documentBuilder.reset();
        documentBuilder.setEntityResolver(MCREntityResolver.instance());
        return documentBuilder;
    }

    private static DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilder documentBuilder = LazyHolder.INSTANCE.docBuilderFactory.newDocumentBuilder();
        return resetDocumentBuilder(documentBuilder);
    }

    @Override
    public void close() {
        while (!builderQueue.isEmpty()) {
            DocumentBuilder documentBuilder = builderQueue.poll();
            documentBuilder.reset();
            documentBuilder.setEntityResolver(null);
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void prepareClose() {
    }

    private static class LazyHolder {
        private static final MCRDOMUtils INSTANCE = new MCRDOMUtils();
    }

}
