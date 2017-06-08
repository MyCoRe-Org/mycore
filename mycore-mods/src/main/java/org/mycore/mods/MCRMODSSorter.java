/*
 * $Revision: 30923 $ $Date: 2014-10-22 10:54:47 +0200 (Mi, 22 Okt 2014) $
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

package org.mycore.mods;

import java.util.Arrays;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.xml.MCRURIResolver;

/**
 * Provides functionality to sort MODS elements to a predefined order.
 * The MODSSorter can be either used as URIResolver via 
 * "sort:[...URI returning MODS...]"
 * or by invoking 
 * MCRMODSSorter.sort( [JDOM Element with mods:mods] );
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRMODSSorter implements URIResolver {

    private final static String[] order = { "genre", "typeofResource", "titleInfo", "nonSort", "subTitle", "title",
            "partNumber", "partName", "name", "namePart", "displayForm", "role", "affiliation", "originInfo", "place",
            "publisher", "dateIssued", "dateCreated", "dateModified", "dateValid", "dateOther", "edition", "issuance",
            "frequency", "relatedItem", "language", "physicalDescription", "abstract", "note", "subject",
            "classification", "location", "shelfLocator", "url", "accessCondition", "part", "extension", "recordInfo" };

    private final static List<String> orderList = Arrays.asList(order);

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        href = href.substring(href.indexOf(":") + 1);
        Element mods = MCRURIResolver.instance().resolve(href);
        MCRMODSSorter.sort(mods);
        return new JDOMSource(mods);
    }

    public static void sort(Element mods) {
        mods.sortChildren((Element e1, Element e2) -> compare(e1, e2));
    }

    private static int compare(Element e1, Element e2) {
        int pos1 = getPos(e1);
        int pos2 = getPos(e2);

        if (pos1 == pos2)
            return e1.getName().compareTo(e2.getName());
        else
            return pos1 - pos2;
    }

    private static int getPos(Element e) {
        String name = e.getName();
        return orderList.contains(name) ? orderList.indexOf(name) : orderList.size();
    }
}
