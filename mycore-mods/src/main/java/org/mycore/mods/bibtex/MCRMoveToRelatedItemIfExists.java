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
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;

import bibtex.dom.BibtexEntry;

/**
 * Some bibTeX fields must be moved "up" to the mods:relatedItem if a given XPath condition matches, e.g. "here is a host".
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRMoveToRelatedItemIfExists extends MCRFieldTransformer {

    private String xPathOfRelatedItem;

    private MCRFieldTransformer wrappedTransformer;

    MCRMoveToRelatedItemIfExists(String xPathOfRelatedItem, MCRFieldTransformer wrappedTransformer) {
        super(wrappedTransformer.field);
        this.xPathOfRelatedItem = xPathOfRelatedItem;
        this.wrappedTransformer = wrappedTransformer;
    }

    @Override
    void transformField(BibtexEntry entry, Element parent) {
        Element target = getRelatedItemIfExists(parent);
        wrappedTransformer.transformField(entry, target);
    }

    Element getRelatedItemIfExists(Element parent) {
        XPathExpression<Element> xPath = XPathFactory.instance().compile(xPathOfRelatedItem, Filters.element(), null,
            MCRConstants.getStandardNamespaces());
        Element fixedParent = xPath.evaluateFirst(parent);
        return fixedParent != null ? fixedParent : parent;
    }
}
