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

package org.mycore.tei;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;

/**
 * Can split a TEI document into multiple documents based on the pb elements.
 */
public class MCRTEISplitter {
    
    private final TeiFile original;
    
    private Element copyTarget;

    private List<TeiFile> splitDocumentList = new ArrayList<>();
    
    private int size = -1;

    public MCRTEISplitter(TeiFile original) {
        this.original = original;
    }

    /**
     * Checks if the document is splitable. A document is splitable if it contains pb elements.
     * @return true if the document is splitable, false otherwise
     */
    public boolean isSplitable() {
        XPathFactory xFactory = XPathFactory.instance();
        XPathExpression<Element> expr = xFactory.compile("//tei:pb", Filters.element(), null,
            MCRConstants.TEI_NAMESPACE);
        List<Element> elementList = expr.evaluate(this.original.doc());
        return !elementList.isEmpty();
    }

    /**
     * Returns the estimated size of the split documents. This is the number of pb
     * @return the estimated size of the split documents
     */
    public int getEstimatedSize() {
        if (size == -1) {
            XPathFactory xFactory = XPathFactory.instance();
            XPathExpression<Element> expr = xFactory.compile("//tei:pb", Filters.element(), null,
                MCRConstants.TEI_NAMESPACE);
            List<Element> elementList = expr.evaluate(this.original.doc());
            size = elementList.size();
        }

        return size;
    }

    private Stub copyAncestors(Element pbElement, String name) {
        Element parent = pbElement;
        Element lastClone = null;
        Element firstClone = null;
        while ((parent = parent.getParentElement()) != null) {
            Element cloned = cloneElement(parent);
            if (firstClone == null) {
                firstClone = cloned;
            }
            if (lastClone != null) {
                cloned.addContent(lastClone);
            }
            lastClone = cloned;
        }

        TeiFile teiFile = new TeiFile(name, new Document(lastClone));
        this.splitDocumentList.add(teiFile);
        return new Stub(firstClone, teiFile);
    }

    private void traverse(Element element) {
        for (Content content : element.getContent()) {
            if (content instanceof Element contentElement && contentElement.getName().equals("pb")) {
                String facs = contentElement.getAttributeValue("facs");
                if (facs.startsWith("images/")) {
                    facs = facs.substring("images/".length());
                }
                Stub newStub = copyAncestors(element, facs);
                copyTarget = newStub.newEl;
                continue;
            }

            copyToNew(content);
        }
        copyTarget = copyTarget.getParentElement();
    }

    private void copyToNew(Content content) {
        if (content instanceof Element elementContent) {
            Element cloned = cloneElement(elementContent);
            copyTarget.addContent(cloned);

            copyTarget = cloned;
            traverse(elementContent);
        } else {
            copyTarget.addContent(content.clone());
        }

    }

    private Element cloneElement(Element elementContent) {
        Element element = new Element(elementContent.getName(), elementContent.getNamespace());
        elementContent.getAttributes()
            .stream()
            .map(Attribute::clone)
            .forEach(element::setAttribute);

        return element;
    }

    /**
     * Splits the document into multiple documents based on the pb elements.
     * @return a list of the split documents
     */
    public List<TeiFile> split() {
        if (!splitDocumentList.isEmpty()) {
            splitDocumentList = new ArrayList<>();
        }

        Element originalText = this.original.doc()
            .getRootElement().getChild("text", MCRConstants.TEI_NAMESPACE);

        Stub newStub = copyAncestors(originalText.getChildren().get(0), null);
        copyTarget = newStub.newEl;

        traverse(originalText);

        return splitDocumentList;
    }

    /**
     * Represents a TEI file.
     */
    public record TeiFile(String name, Document doc) {
    }

    private record Stub(Element newEl, TeiFile teiFile) {
    }
}
