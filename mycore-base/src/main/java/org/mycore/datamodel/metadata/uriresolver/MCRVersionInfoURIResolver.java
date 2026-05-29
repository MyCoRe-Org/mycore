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

package org.mycore.datamodel.metadata.uriresolver;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.common.MCRAbstractMetadataVersion;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * {@link URIResolver} that returns the version history of a object as an XML source.
 */
public class MCRVersionInfoURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Resolves the version history of the given object ID and returns it as an XML source.
     * <p>If the metadata store supports revisions, all revisions are returned. Otherwise,
     * a single {@code <version>} element with the last modification timestamp is returned.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{mcrId}
     * </pre>
     * <p>Example request:
     * <pre>
     *   versioninfo:mcr_document_00000001
     * </pre>
     * <p>Example response with revisions:
     * <pre>{@code
     *   <versions>
     *     <version user="admin" date="2024-01-01T00:00:00Z" r="1" action="A"/>
     *     <version user="editor" date="2024-06-01T12:00:00Z" r="2" action="M"/>
     *   </versions>
     * }</pre>
     * <p>Example response without revision support:
     * <pre>{@code
     *   <versions>
     *     <version date="2024-06-01T12:00:00Z"/>
     *   </versions>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping the {@code <versions>} element
     * @throws TransformerException if the version history cannot be read
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String id = href.substring(href.indexOf(':') + 1);
        LOGGER.debug("Reading version info of MCRObject with ID {}", id);
        MCRObjectID mcrId = MCRObjectID.getInstance(id);
        MCRXMLMetadataManager metadataManager = MCRXMLMetadataManager.obtainInstance();
        try {
            List<? extends MCRAbstractMetadataVersion<?>> versions = metadataManager.listRevisions(mcrId);
            if (versions != null && !versions.isEmpty()) {
                return getSource(versions);
            } else {
                return getSource(Instant.ofEpochMilli(metadataManager.getLastModified(mcrId))
                    .truncatedTo(ChronoUnit.MILLIS));
            }
        } catch (Exception e) {
            throw new TransformerException(e);
        }
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private Source getSource(Instant lastModified) {
        Element e = new Element("versions");
        Element v = new Element("version");
        e.addContent(v);
        v.setAttribute("date", lastModified.toString());
        return new JDOMSource(e);
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private Source getSource(List<? extends MCRAbstractMetadataVersion<?>> versions) {
        Element e = new Element("versions");
        for (MCRAbstractMetadataVersion<?> version : versions) {
            Element v = new Element("version");
            v.setAttribute("user", version.getUser());
            v.setAttribute("date", MCRXMLFunctions.getISODate(version.getDate(), null));
            v.setAttribute("r", version.getRevision());
            v.setAttribute("action", Character.toString(version.getType()));
            e.addContent(v);
        }
        return new JDOMSource(e);
    }

}
