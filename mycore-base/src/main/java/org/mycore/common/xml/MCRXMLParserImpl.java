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

package org.mycore.common.xml;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderJDOMFactory;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.EntityResolver2;

/**
 * Parses XML content using specified {@link XMLReaderJDOMFactory}.
 *
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRXMLParserImpl implements MCRXMLParser {

    private static final String FEATURE_NAMESPACES = "http://xml.org/sax/features/namespaces";

    private static final String FEATURE_SCHEMA_SUPPORT = "http://apache.org/xml/features/validation/schema";

    private static final String FEATURE_FULL_SCHEMA_SUPPORT =
        "http://apache.org/xml/features/validation/schema-full-checking";

    private boolean validate;

    private SAXBuilder builder;

    public MCRXMLParserImpl(XMLReaderJDOMFactory factory) {
        this(factory, false);
    }

    public MCRXMLParserImpl(XMLReaderJDOMFactory factory, boolean silent) {
        this.validate = factory.isValidating();
        builder = new SAXBuilder(factory);
        builder.setFeature(FEATURE_NAMESPACES, true);
        builder.setFeature(FEATURE_SCHEMA_SUPPORT, validate);
        builder.setFeature(FEATURE_FULL_SCHEMA_SUPPORT, false);
        builder.setErrorHandler(new MCRXMLParserErrorHandler(silent));
        builder.setEntityResolver(new AbsoluteToRelativeResolver(MCREntityResolver.instance()));
    }

    @Override
    public boolean isValidating() {
        return validate;
    }

    @Override
    public Document parseXML(MCRContent content) throws IOException, JDOMException {
        InputSource source = content.getInputSource();
        return builder.build(source);
    }

    @Override
    public XMLReader getXMLReader() throws SAXException, ParserConfigurationException {
        try {
            return builder.getXMLReaderFactory().createXMLReader();
        } catch (JDOMException ignoredWithSAXCause) {
            Throwable cause = ignoredWithSAXCause.getCause();
            if (cause instanceof SAXException se) {
                throw se;
            }
            if (cause instanceof ParserConfigurationException pce) {
                throw pce;
            }
            throw new MCRException(ignoredWithSAXCause);
        }
    }

    /**
     * Xerces 2.11.0 does not provide a relative systemId if baseURI is a XML file to be validated by a schema specified
     * in systemId. This EntityResolver makes a relative systemId so that the fallback could conform to the defined
     * interface.
     *
     * @author Thomas Scheffler (yagee)
     */
    private static class AbsoluteToRelativeResolver implements EntityResolver2 {

        private final EntityResolver2 fallback;

        private static final Logger LOGGER = LogManager.getLogger();

        private static final URI BASE_DIR_URI = Paths.get("").toAbsolutePath().toUri();

        AbsoluteToRelativeResolver(EntityResolver2 fallback) {
            this.fallback = fallback;
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            return fallback.resolveEntity(publicId, systemId);
        }

        @Override
        public InputSource getExternalSubset(String name, String baseURI) throws SAXException, IOException {
            return fallback.getExternalSubset(name, baseURI);
        }

        @Override
        public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId)
            throws SAXException, IOException {
            if (baseURI == null) {
                //check if baseDirURI is part of systemID and seperate
                try {
                    String relativeURI = BASE_DIR_URI.relativize(URI.create(systemId)).toString();
                    if (!systemId.equals(relativeURI)) {
                        return resolveEntity(name, publicId, BASE_DIR_URI.toString(), relativeURI);
                    }
                } catch (RuntimeException e) {
                    LOGGER.debug("Could not separate baseURI from {}", systemId, e);
                }
            }
            String relativeSystemId = relativize(baseURI, systemId);
            if (relativeSystemId.equals(systemId)) {
                LOGGER.debug("Try to use EntityResolver interface");
                InputSource inputSource = resolveEntity(publicId, systemId);
                if (inputSource != null) {
                    LOGGER.debug("Found resource in EntityResolver interface");
                    return inputSource;
                }
            }
            return fallback.resolveEntity(name, publicId, baseURI, relativeSystemId);
        }

        private static String relativize(String baseURI, String systemId) {
            if (baseURI == null) {
                return systemId;
            }
            String normalizedBaseURI = normalize(baseURI);
            int pos = normalizedBaseURI.lastIndexOf('/');
            String prefix = normalizedBaseURI.substring(0, pos + 1);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("prefix of baseURI ''{}'' is: {}", normalizedBaseURI, prefix);
                LOGGER.debug("systemId: {} prefixed? {}", systemId, systemId.startsWith(prefix));
            }
            if (prefix.length() > 0 && systemId.startsWith(prefix)) {
                String systemIdSub = systemId.substring(prefix.length());
                LOGGER.debug("new systemId: {}", systemIdSub);
                return systemIdSub;
            }
            return systemId;
        }

        private static String normalize(String baseURI) {
            try {
                return URI.create(baseURI).normalize().toString();
            } catch (RuntimeException e) {
                LOGGER.debug("Error while normalizing {}", baseURI, e);
                return baseURI;
            }
        }

    }
}
