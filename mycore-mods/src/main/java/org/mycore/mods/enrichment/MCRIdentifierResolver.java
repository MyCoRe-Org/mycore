/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.mods.enrichment;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.mods.MCRMODSCommands;
import org.xml.sax.SAXException;

/**
 * Returns publication data in MODS format for a given identifier.
 * Each resolver belongs to a certain data source, e.g. the data source
 * "PubMed" may have two resolves to get publication data by DOI or PubMed artice ID.
 *
 * The resolver will use an URI to get the publication data.
 * MCR.MODS.EnrichmentResolver.DataSource.[SourceID].[TypeID].URI=[URI]
 *
 * This is typically a HTTP URL followed by a XSL stylesheet to transform the
 * source format to MODS, e.g.
 * MCR.MODS.EnrichmentResolver.DataSource.DataCite.doi.URI=xslStyle:datacite2mods:https://data.datacite.org/application/vnd.datacite.datacite+xml/{0}
 *
 * Within the URI, the pattern {0} will be replaced by the given identifier value,
 * optionally the pattern {1} will be replaced by the value uri-encoded as http request parameter
 *
 * @author Frank Lützenkirchen
 */
class MCRIdentifierResolver {

    private static final Logger LOGGER = LogManager.getLogger(MCRIdentifierResolver.class);

    private MCRDataSource ds;

    private MCRIdentifierType idType;

    private String uriPattern;

    MCRIdentifierResolver(MCRDataSource ds, MCRIdentifierType idType, String uriPattern) {
        this.ds = ds;
        this.idType = idType;
        this.uriPattern = uriPattern;
    }

    MCRIdentifierType getType() {
        return idType;
    }

    /**
     * Tries to resolve publication data for the given identifier.
     *
     * @param identifier the identifier's value, e.g. a DOI or ISBN
     * @return the publication data in MODS format, or null if the data source did not return data for this identifier
     */
    Element resolve(String identifier) {
        Object[] params = new Object[] { identifier, URLEncoder.encode(identifier, StandardCharsets.UTF_8) };
        String uri = new MessageFormat(uriPattern, Locale.ROOT).format(params);

        Element resolved = null;
        try {
            resolved = MCRURIResolver.instance().resolve(uri);
        } catch (Exception ex) {
            LOGGER.warn("Exception resolving " + uri, ex);
            return null;
        }

        // Normalize various error/not found cases
        if (resolved == null || !"mods".equals(resolved.getName()) || resolved.getChildren().isEmpty()) {
            LOGGER.warn(ds + " returned none or empty MODS for " + idType + " " + identifier);
            return null;
        }

        try {
            ensureIsValidMODS(resolved);
            return resolved;
        } catch (Exception ex) {
            LOGGER.warn(ds + " returned invalid MODS for " + identifier + ": " + ex.getMessage(), ex);
            return null;
        }
    }

    void ensureIsValidMODS(Element mods) throws SAXException, IOException {
        MCRXMLHelper.validate(new Document().addContent(mods.detach()), MCRMODSCommands.MODS_V3_XSD_URI);
    }
}
