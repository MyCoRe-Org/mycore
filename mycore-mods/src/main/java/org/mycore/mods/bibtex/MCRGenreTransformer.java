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

package org.mycore.mods.bibtex;

import java.util.Locale;

import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;

import bibtex.dom.BibtexEntry;

/**
 * Transforms the BibTeX entry type to mods:genre element(s).
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRGenreTransformer {

    static void setGenre(BibtexEntry entry, Element mods) {
        String type = entry.getEntryType().toLowerCase(Locale.ROOT);
        MCRFieldTransformer.buildElement("mods:genre", type, mods);
    }

    static void fixHostGenre(BibtexEntry entry, Element mods) {
        String type = entry.getEntryType().toLowerCase(Locale.ROOT);
        if ("incollection".equals(type) || "inproceedings".equals(type) || "inbook".equals(type)) {
            type = type.substring(2);

            Element genre = getHostGenre(mods);
            if (genre != null) {
                genre.setText(type);
            } else {
                MCRFieldTransformer.buildElement("mods:relatedItem[@type='host']/mods:genre", type, mods);
            }
        }
    }

    private static Element getHostGenre(Element mods) {
        XPathExpression<Element> expr = XPathFactory.instance().compile("mods:relatedItem[@type='host']/mods:genre",
            Filters.element(), null, MCRConstants.getStandardNamespaces());
        Element genre = expr.evaluateFirst(mods);
        return genre;
    }
}
