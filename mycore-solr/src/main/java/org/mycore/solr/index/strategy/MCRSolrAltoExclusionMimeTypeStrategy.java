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
package org.mycore.solr.index.strategy;

import java.io.InputStream;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.xml.MCRXMLFunctions;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This strategy checks, if the current file is an Alto file and excludes it from SOLR index handler.
 * Otherwise it delegates the check to the MCRSolrMimeTypeStrategy (which is is primarily used to ignore images).
 * 
 * Be aware that this is the ignore pattern, the {@link #check(Path, BasicFileAttributes)} 
 * method will return false if it matches.
 * 
 * @author Sebastian Hofmann
 * @author Robert Stephan
 */
public class MCRSolrAltoExclusionMimeTypeStrategy extends MCRSolrMimeTypeStrategy {
    private final static List<String> XML_MIME_TYPES = Arrays.asList("application/xml", "text/xml");

    public static final String ALTO_ROOT = "alto";

    @Override
    public boolean check(Path file, BasicFileAttributes attrs) {
        String fileName = file.getFileName().toString();
        if (fileName.endsWith(".alto.xml") || fileName.endsWith(".alto")) {
            return false;
        }
        String mimeType = MCRXMLFunctions.getMimeType(fileName);
        if (XML_MIME_TYPES.contains(mimeType)) {
            final String localRootName = getLocalRootName(file).orElse(null);
            if (Objects.equals(localRootName, ALTO_ROOT)) {
                return false;
            }
        }
        return super.check(file, attrs);
    }

    private static Optional<String> getLocalRootName(Path path) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            SAXParser saxParser = factory.newSAXParser();
            ProbeXMLHandler handler = new ProbeXMLHandler();
            try (InputStream is = Files.newInputStream(path)) {
                saxParser.parse(is, handler);
            }
        } catch (ProbeXMLException probeExc) {
            return Optional.of(probeExc.rootName);
        } catch (Exception exc) {
            LogManager.getLogger().warn("unable to probe root node of  {}", path);
        }
        return Optional.empty();
    }

    private static final class ProbeXMLHandler extends DefaultHandler {

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            throw new ProbeXMLException(localName);
        }

    }

    private static final class ProbeXMLException extends RuntimeException {

        @Serial
        private static final long serialVersionUID = 1L;

        private String rootName;

        private ProbeXMLException(String rootName) {
            this.rootName = rootName;
        }

    }
}
