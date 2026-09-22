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

import java.util.Objects;
import java.util.function.Supplier;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.xsl.uriresolver.MCRURIResolver;

/**
 * Manages access to one application-scoped {@link SAXTransformerFactory}.
 * <p>
 * JAXP does not guarantee that a factory supports concurrent operations. Access can therefore be serialized for
 * providers that require it, while the returned {@link Templates}, {@link Transformer}, and
 * {@link TransformerHandler} instances can follow their individual JAXP lifecycle rules.
 */
public final class MCRTransformerFactory {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String id;

    private final SAXTransformerFactory factory;

    private final boolean supportsConcurrency;

    private final Supplier<URIResolver> uriResolverSupplier;

    private final Object factoryMonitor = new Object();

    private final Object initializationMonitor = new Object();

    private volatile boolean initialized;

    private boolean initializing;

    MCRTransformerFactory(String id, SAXTransformerFactory factory, boolean supportsConcurrency) {
        this(id, factory, supportsConcurrency, MCRURIResolver::obtainInstance);
    }

    MCRTransformerFactory(String id, SAXTransformerFactory factory, boolean supportsConcurrency,
        Supplier<URIResolver> uriResolverSupplier) {
        this.id = Objects.requireNonNull(id);
        this.factory = Objects.requireNonNull(factory);
        this.supportsConcurrency = supportsConcurrency;
        this.uriResolverSupplier = Objects.requireNonNull(uriResolverSupplier);
    }

    /**
     * Returns the shared factory for the default selected at call time.
     *
     * @return shared factory selected by {@link MCRTransformerFactorySelector#getDefaultFactoryId()}
     */
    public static MCRTransformerFactory getSharedFactory() {
        return MCRTransformerFactoryRegistry.obtainInstance().getSharedFactory();
    }

    /**
     * Returns the shared factory with the supplied ID.
     *
     * @param id configured factory ID
     * @return shared factory for this ID
     */
    public static MCRTransformerFactory getSharedFactory(String id) {
        return MCRTransformerFactoryRegistry.obtainInstance().getSharedFactory(id);
    }

    /**
     * Returns the uniquely configured factory with the supplied implementation class.
     *
     * @deprecated use {@link #getSharedFactory(String)} with a configured factory ID
     */
    @Deprecated(forRemoval = true)
    @SuppressWarnings("removal")
    public static MCRTransformerFactory getFactory(Class<? extends TransformerFactory> factoryClass) {
        return MCRTransformerFactoryRegistry.obtainInstance().getFactory(factoryClass);
    }

    /**
     * Returns the configured ID of this manager.
     *
     * @return configured factory ID
     */
    public String getId() {
        return id;
    }

    /**
     * Compiles a stylesheet using the configured factory access policy.
     */
    public Templates newTemplates(Source source) throws TransformerConfigurationException {
        return withFactoryAccess(() -> factory.newTemplates(source));
    }

    /**
     * Creates a request-local transformer handler using the configured factory access policy.
     */
    public TransformerHandler newTransformerHandler(Templates templates) throws TransformerConfigurationException {
        return withFactoryAccess(() -> factory.newTransformerHandler(templates));
    }

    /**
     * Compiles a stylesheet and creates a request-local transformer using the configured factory access policy.
     */
    public Transformer newTransformer(Source source) throws TransformerConfigurationException {
        return withFactoryAccess(() -> factory.newTransformer(source));
    }

    /**
     * Creates a request-local identity transformer using the configured factory access policy.
     */
    public Transformer newTransformer() throws TransformerConfigurationException {
        return withFactoryAccess(factory::newTransformer);
    }

    boolean supportsConcurrency() {
        return supportsConcurrency;
    }

    private <T> T withFactoryAccess(FactoryOperation<T> operation) throws TransformerConfigurationException {
        initialize();
        if (supportsConcurrency) {
            return operation.execute();
        }
        synchronized (factoryMonitor) {
            return operation.execute();
        }
    }

    public Class<? extends TransformerFactory> getFactoryClass() {
        return factory.getClass();
    }

    @SuppressWarnings("PMD.UnusedAssignment") // read by a same-thread recursive call through the resolver supplier
    private void initialize() {
        if (initialized) {
            return;
        }
        synchronized (initializationMonitor) {
            if (initialized) {
                return;
            }
            if (initializing) {
                throw new MCRConfigurationException("Recursive initialization of transformer factory manager '"
                    + id + "'");
            }
            initializing = true;
            try {
                factory.setURIResolver(uriResolverSupplier.get());
                factory.setErrorListener(new FactoryErrorListener());
                initialized = true;
            } finally {
                initializing = false;
            }
        }
    }

    @FunctionalInterface
    private interface FactoryOperation<T> {

        T execute() throws TransformerConfigurationException;

    }

    private static final class FactoryErrorListener implements ErrorListener {

        @Override
        public void warning(TransformerException exception) {
            TransformerException unwrappedException = MCRErrorListener.unwrapException(exception);
            LOGGER.warn("Warning while compiling XSL stylesheet:{}", unwrappedException::getMessageAndLocation);
        }

        @Override
        public void error(TransformerException exception) throws TransformerException {
            TransformerException unwrappedException = MCRErrorListener.unwrapException(exception);
            LOGGER.error("Error while compiling XSL stylesheet:{}", unwrappedException::getMessageAndLocation);
            throw unwrappedException;
        }

        @Override
        public void fatalError(TransformerException exception) throws TransformerException {
            TransformerException unwrappedException = MCRErrorListener.unwrapException(exception);
            LOGGER.fatal("Fatal error while compiling XSL stylesheet.", unwrappedException);
            throw unwrappedException;
        }

    }

}
