/*
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mods.enrichment;

import java.net.URLEncoder;
import java.text.MessageFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.xml.MCRURIResolver;

/**
 * @author Frank L\u00FCtzenkirchen
 */
class MCRIdentifierResolver {

    private static final Logger LOGGER = LogManager.getLogger(MCRIdentifierResolver.class);

    private MCRIdentifierType idType;

    private String uriPattern;

    public MCRIdentifierResolver(MCRIdentifierType idType, String uriPattern) {
        this.idType = idType;
        this.uriPattern = uriPattern;
    }

    public MCRIdentifierType getType() {
        return idType;
    }

    public Element resolve(String identifier) {
        Element resolved = null;
        try {
            String uri = MessageFormat.format(uriPattern, identifier, URLEncoder.encode(identifier, "UTF-8"));
            resolved = MCRURIResolver.instance().resolve(uri);
        } catch (Exception ex) {
            LOGGER.warn("Exception resolving " + identifier + ": " + ex.getClass().getName() + " " + ex.getMessage());
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
