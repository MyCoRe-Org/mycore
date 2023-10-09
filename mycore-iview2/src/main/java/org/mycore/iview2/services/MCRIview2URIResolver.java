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

package org.mycore.iview2.services;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

public class MCRIview2URIResolver implements URIResolver {
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String[] params = href.split(":");

        if (params.length != 3) {
            throw new TransformerException("Invalid href: " + href);
        }

        switch (params[1]) {
            case "isCompletelyTiled" -> {
                boolean completelyTiled = MCRIView2Tools.isCompletelyTiled(params[2]);
                return new JDOMSource(new Element(String.valueOf(completelyTiled)));
            }
            default -> throw new TransformerException("Invalid href: " + href);
        }
    }
}
