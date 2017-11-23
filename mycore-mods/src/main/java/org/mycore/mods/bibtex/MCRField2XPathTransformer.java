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

import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexString;

/**
 * Generic implementation that transforms a single BibTeX field to a given MODS structure expressed as XPath .
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRField2XPathTransformer extends MCRFieldTransformer {

    protected String xPath;

    MCRField2XPathTransformer(String field, String xPath) {
        super(field);
        this.xPath = xPath;
    }

    void buildField(BibtexAbstractValue value, Element parent) {
        String content = ((BibtexString) value).getContent();
        content = normalizeValue(content);
        buildElement(xPath, content, parent);
    }
}
