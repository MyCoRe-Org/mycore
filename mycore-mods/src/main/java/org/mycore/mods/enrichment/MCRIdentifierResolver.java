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

import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.xml.MCRURIResolver;

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
 * @author Frank L\u00FCtzenkirchen
 */
class MCRIdentifierResolver {

    private static final Logger LOGGER = LogManager.getLogger(MCRIdentifierResolver.class);

    private MCRIdentifierType idType;

    private String uriPattern;

    MCRIdentifierResolver(MCRIdentifierType idType, String uriPattern) {
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
        Element resolved = null;
        try {
            String uri = new MessageFormat(uriPattern, Locale.ROOT)
                .format(new Object[] { identifier, URLEncoder.encode(identifier, "UTF-8") });
            resolved = MCRURIResolver.instance().resolve(uri);
        } catch (Exception ex) {
            LOGGER.info("Exception resolving {}: {} {}", identifier, ex.getClass().getName(), ex.getMessage());
        }

        // Normalize various error/not found cases:
        if (resolved == null) {
            return null;
        } else if (!"mods".equals(resolved.getName())) {
            return null;
        } else if (resolved.getChildren().isEmpty()) {
            return null;
        } else {
            return resolved;
        }
    }
}
