/**
 * $Revision: 23345 $ 
 * $Date: 2012-01-30 12:08:41 +0100 (Mo, 30 Jan 2012) $
 *
 * This file is part of the MILESS repository software.
 * Copyright (C) 2011 MILESS/MyCoRe developer team
 * See http://duepublico.uni-duisburg-essen.de/ and http://www.mycore.de/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/

package org.mycore.user2;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom.transform.JDOMSource;
import org.mycore.user2.utils.MCRUserTransformer;

/**
 * Implements URIResolver for use in editor form user-editor.xml
 *  
 * user:{userID}
 *   returns detailed user data including owned users and groups
 * user:current
 *   returns detailed user data of the user currently logged in
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRUserResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String userID = href.split(":")[1];
        MCRUser user = null;
        if ("current".equals(userID)) {
            user = MCRUserManager.getCurrentUser();
        } else {
            user = MCRUserManager.getUser(userID);
        }
        if (user == null) {
            return null;
        }
        return new JDOMSource(MCRUserTransformer.buildXML(user));
    }
}
