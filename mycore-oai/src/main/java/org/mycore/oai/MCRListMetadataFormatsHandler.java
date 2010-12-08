/*
 * $Revision$ 
 * $Date$
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

package org.mycore.oai;

import static org.mycore.oai.MCROAIConstants.ARG_IDENTIFIER;
import static org.mycore.oai.MCROAIConstants.V_OPTIONAL;

import java.util.List;
import java.util.Properties;

/**
 * Implements the ListMetadataFormats request as defined in section 4.4 of the OAI-PMH specification.
 * This verb is used to retrieve the metadata formats available from a repository. 
 * An optional argument restricts the request to the formats available for a specific item.
 * 
 * When no item identifier is given in the request, this just returns all the metadata formats 
 * configured for the data provider instance.
 * 
 * When the optional item identifier is given in the request, 
 * the MCROAIAdapter's listMetadataFormats method is called 
 * to decide which formats are available for that identifier.  
 * 
 * @see MCRMetadataFormat
 * @see MCROAIDataProvider#getMetadataFormats()
 * @see MCROAIAdapter#listMetadataFormats(String, List)
 * 
 * @author Frank L\u00fctzenkirchen
 */
class MCRListMetadataFormatsHandler extends MCRVerbHandler {
    final static String VERB = "ListMetadataFormats";

    void setAllowedParameters(Properties p) {
        p.setProperty(ARG_IDENTIFIER, V_OPTIONAL);
    }

    MCRListMetadataFormatsHandler(MCROAIDataProvider provider) {
        super(provider);
    }

    void handleRequest() {
        String identifier = parms.getProperty(ARG_IDENTIFIER);
        List<MCRMetadataFormat> formats = null;

        if (identifier == null)
            formats = provider.getMetadataFormats();
        else if (checkIdentifier(identifier))
            formats = provider.getAdapter().listMetadataFormats(identifier, provider.getMetadataFormats());
        else
            return;

        for (MCRMetadataFormat format : formats)
            output.addContent(format.buildXML());
    }
}
