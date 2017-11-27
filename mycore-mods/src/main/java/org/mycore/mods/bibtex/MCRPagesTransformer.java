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
