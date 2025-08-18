/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.frontend.xeditor.tracker;

import java.util.List;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;

/**
 * Sets an element's text in the edited xml, and tracks that change.  
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRSetElementText extends MCRChange {

    public String encodedOldContent;

    public MCRSetElementText(Element element, String newValue) {
        super(element);
        setMessage(String.format("Set %s=\"%s\"", getXPath(), newValue));
        this.encodedOldContent = encodeContent(element);
        element.setText(newValue);
    }

    private String encodeContent(Element element) {
        List<Content> content = element.cloneContent();
        Element x = new Element("x").setContent(content);
        return MCRElementEncoder.element2text(x);
    }

    @Override
    protected void undo(Document doc) {
        Element element = (Element) (getNodeByXPath(doc));
        element.setContent(decodeContent());
    }

    private List<Content> decodeContent() {
        Element x = MCRElementEncoder.text2element(encodedOldContent);
        return x.removeContent();
    }
}
