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
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;

import bibtex.dom.BibtexEntry;

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
        parent = getRelatedItemIfExists(parent);
        wrappedTransformer.transformField(entry, parent);
    }

    Element getRelatedItemIfExists(Element parent) {
        XPathExpression<Element> xPath = XPathFactory.instance().compile(xPathOfRelatedItem, Filters.element(), null,
                MCRConstants.getStandardNamespaces());
        Element fixedParent = xPath.evaluateFirst(parent);
        return fixedParent != null ? fixedParent : parent;
    }
}