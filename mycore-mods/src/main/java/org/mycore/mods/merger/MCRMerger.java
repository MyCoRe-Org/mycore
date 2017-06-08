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

package org.mycore.mods.merger;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.xml.MCRXMLHelper;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRMerger {

    protected Element element;

    public void setElement(Element element) {
        this.element = element;
    }

    public boolean isProbablySameAs(MCRMerger other) {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MCRMerger)
            return MCRXMLHelper.deepEqual(this.element, ((MCRMerger) obj).element);
        else
            return super.equals(obj);
    }

    public void mergeFrom(MCRMerger other) {
        mergeAttributes(other);
        mergeElements(other);
    }

    protected void mergeAttributes(MCRMerger other) {
        for (Attribute attribute : other.element.getAttributes()) {
            if (this.element.getAttribute(attribute.getName(), attribute.getNamespace()) == null)
                this.element.setAttribute(attribute.clone());
        }
    }

    protected void mergeElements(MCRMerger other) {
        List<MCRMerger> entries = new ArrayList<MCRMerger>();
        for (Element child : this.element.getChildren())
            entries.add(MCRMergerFactory.buildFrom(child));

        for (Element child : other.element.getChildren())
            mergeIntoExistingEntries(entries, MCRMergerFactory.buildFrom(child));
    }

    private void mergeIntoExistingEntries(List<MCRMerger> entries, MCRMerger newEntry) {
        for (MCRMerger existingEntry : entries) {
            if (newEntry.equals(existingEntry))
                return;
            else if (newEntry.isProbablySameAs(existingEntry)) {
                existingEntry.mergeFrom(newEntry);
                return;
            }
        }
        entries.add(newEntry);
        element.addContent(newEntry.element.clone());
    }

    /** Holds the MODS namespace */
    private static List<Namespace> NS = new ArrayList<Namespace>();

    static {
        NS.add(Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3"));
    }

    protected List<Element> getNodes(String xPath) {
        XPathExpression<Element> xPathExpr = XPathFactory.instance().compile(xPath, Filters.element(), null, NS);
        return xPathExpr.evaluate(element);
    }
}
