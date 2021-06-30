/*
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

package org.mycore.accesskey;

import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;

import org.mycore.accesskey.backend.MCRAccessKey;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Returns a JSON-String with {@link MCRAccessKey} for an given {@link MCRObjectID}.
 */
public class MCRAccessKeyResolver implements URIResolver {
    
    /* (non-Javadoc)
     * @see javax.xml.transform.URIResolver#resolve(java.lang.String, java.lang.String)
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        final MCRObjectID objectId = MCRObjectID.getInstance(href.substring(href.indexOf(":") + 1));
        final List<MCRAccessKey> accessKeys = MCRAccessKeyManager.getAccessKeys(objectId);

        if (accessKeys.size() != 0) {
            return new JDOMSource(MCRAccessKeyTransformer.servFlagFromAccessKeys(accessKeys));
        }
        return new JDOMSource(new Element("null"));
    }
}
