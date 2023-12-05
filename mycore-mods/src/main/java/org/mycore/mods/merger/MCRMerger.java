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

package org.mycore.mods.merger;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.xml.MCRXMLHelper;

/**
 * MCRMerger is the main and default implementation for comparing
 * and merging MODS elements that are semantically the same.
 * Each MODS element is wrapped by an instance of MCRMerger or one of its subclasses.
 * It contains methods to decide whether the two MODS elements are equal, or probably represent the information
 * maybe with different granularity.
 * If so, the text, elements and attributes are merged so that the "better" information/representation wins.
 * This is done recursively for all child elements, too.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRMerger {

    /** Holds the MODS namespace */
    private static List<Namespace> NS = Collections.singletonList(MCRConstants.MODS_NAMESPACE);

    /** The MODS element wrapped and compared by this merger */
    protected Element element;

    /** Sets the MODS element wrapped and compared by this merger */
    public void setElement(Element element) {
        Objects.requireNonNull(element);
        this.element = element;
    }

    /**
     * Returns true, if the element wrapped by this merger probably represents the same information as the other.
     * The default implementation returns false and may be overwritten by subclasses implementing logic
     * for specific MODS elements.
     */
    public boolean isProbablySameAs(MCRMerger other) {
        return false;
    }

    /**
     * Two mergers are equal if they wrap elements that are deep equals.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MCRMerger) {
            MCRMerger other = (MCRMerger) obj;
            return sameElementName(other) && MCRXMLHelper.deepEqual(this.element, other.element);
        } else {
            return false;
        }
    }
    
    protected boolean sameElementName(MCRMerger other) {
        return this.element.getName().equals(other.element.getName());
    }

    /**
     * Merges the contents of the element wrapped by the other merger into the contents of the element wrapped
     * by this merger.
     * Should only be called if this.isProbablySameAs(other).
     *
     * The default implementation copies all attributes from the other into this if they do not exist in this element.
     * Afterwards it recursively builds mergers for all child elements and compares and eventually merges them too.
     */
    public void mergeFrom(MCRMerger other) {
        mergeAttributes(other);
        mergeElements(other);
    }

    /**
     * Copies those attributes from the other's element into this' element that do not exist in this' element.
     */
    protected void mergeAttributes(MCRMerger other) {
        for (Attribute attribute : other.element.getAttributes()) {
            if (this.element.getAttribute(attribute.getName(), attribute.getNamespace()) == null) {
                this.element.setAttribute(attribute.clone());
            }
        }
    }

    /**
     * Merges all child elements of this with that from other.
     * This is done by building MCRMerger instances for each child element and comparing them.
     */
    protected void mergeElements(MCRMerger other) {
        List<MCRMerger> oldEntries = this.element.getChildren()
            .stream().map(child -> MCRMergerFactory.buildFrom(child)).collect(Collectors.toList());
        List<MCRMerger> newEntries = other.element.getChildren()
            .stream().map(child -> MCRMergerFactory.buildFrom(child)).collect(Collectors.toList());

        for (MCRMerger newEntry : newEntries) {
            MCRMerger matchingEntry = findAndMergeMatch(oldEntries, newEntry);

            if (matchingEntry == null) { // no match found, add as new element
                element.addContent(newEntry.element.clone());
            } else { // match found, do not compare that element with any others further 
                oldEntries.remove(matchingEntry);
            }
        }
    }

    /**
     * Find the entry that is identical or probably the same in the list of given entries.
     * If a non-identical match is found, the new entry is merged into the old one. 
     **/
    private MCRMerger findAndMergeMatch(List<MCRMerger> oldEntries, MCRMerger newEntry) {
        for (MCRMerger oldEntry : oldEntries) {
            // Only same MODS element type can be a match
            if( oldEntry.sameElementName(newEntry) ) {
                if (oldEntry.equals(newEntry)) { // found identical element
                    return oldEntry;
                }
                if (newEntry.isProbablySameAs(oldEntry)) { // found element to merge 
                    oldEntry.mergeFrom(newEntry);
                    return oldEntry;
                }
            }
        }
        return null;
    }

    /**
     * Helper method to lookup child elements by XPath.
     *
     * @param xPath XPath expression relative to the element wrapped by this merger.
     * @return a list of elements matching the given XPath
     */
    protected List<Element> getNodes(String xPath) {
        XPathExpression<Element> xPathExpr = XPathFactory.instance().compile(xPath, Filters.element(), null, NS);
        return xPathExpr.evaluate(element);
    }
}
