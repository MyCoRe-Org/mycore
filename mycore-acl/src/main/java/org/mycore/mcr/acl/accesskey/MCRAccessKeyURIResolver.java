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

package org.mycore.mcr.acl.accesskey;

import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.jdom2.transform.JDOMSource;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyTransformationException;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

/**
 * Returns a JSON string with all {@link MCRAccessKey} for an given {@link MCRObjectID}.
 * <p>Syntax:</p>
 * <ul>
 * <li><code>accesskeys:{objectId}</code> to resolve a access keys as json string and count as attribute</li>
 * </ul>
 */
public class MCRAccessKeyURIResolver implements URIResolver {

    /* (non-Javadoc)
     * @see javax.xml.transform.URIResolver#resolve(java.lang.String, java.lang.String)
     */
    @Override
    public Source resolve(String href, String base) throws MCRAccessKeyTransformationException {
        final MCRObjectID objectId = MCRObjectID.getInstance(href.substring(href.indexOf(":") + 1));
        final List<MCRAccessKey> accessKeys = MCRAccessKeyManager.listAccessKeys(objectId);
        return new JDOMSource(MCRAccessKeyTransformer.elementFromAccessKeys(accessKeys));
    }
}
