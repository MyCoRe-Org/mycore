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

package org.mycore.common.xsl.uriresolver;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.MCRClassTools;

/**
 * Delivers a jdom Element created by any local class that implements URIResolver
 * interface. the class name of the file in the format localclass:org.mycore.ClassName?mode=getAll
 */
public class MCRLocalClassResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String classname = href.substring(href.indexOf(':') + 1, href.indexOf('?'));
        LogManager.getLogger(this.getClass()).debug("Loading Class: {}", classname);
        URIResolver resolver;
        try {
            Class<? extends URIResolver> cl = MCRClassTools.forName(classname);
            resolver = cl.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new TransformerException(e);
        }
        return resolver.resolve(href, base);
    }

}
