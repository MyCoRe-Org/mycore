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

import org.jdom2.Element;
import org.mycore.mods.MCRMODSPagesHelper;

import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexString;

/**
 * Transforms a BibTeX pages field to a JDOM mods:extent element containing pages info.
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRPagesTransformer extends MCRField2XPathTransformer {

    MCRPagesTransformer() {
        super("pages", "mods:relatedItem[@type='host']/mods:part");
    }

    void buildField(BibtexAbstractValue value, Element parent) {
        String pages = ((BibtexString) value).getContent();
        pages = normalizeValue(pages);
        Element part = buildElement(xPath, null, parent);
        part.addContent(MCRMODSPagesHelper.buildExtentPages(pages));
    }
}
