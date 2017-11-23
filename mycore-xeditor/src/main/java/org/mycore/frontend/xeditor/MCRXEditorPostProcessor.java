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

package org.mycore.frontend.xeditor;

import java.io.IOException;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

/**
 * If you implement this interface then you should have a default constructor if you want it to use with.
 * @author Sebastian Hofmann (mcrshofm)
 */
public interface MCRXEditorPostProcessor {
    /**
     * Do the post processing.
     * @param xml the document which has to be post processed
     * @return the post processed document
     * @throws IOException
     * @throws JDOMException
     * @throws SAXException
     */
    Document process(Document xml) throws IOException, JDOMException, SAXException;

    /**
     * Will be called before {@link #process(Document)}.
     * @param attributeMap a map which contains the name(key) and value of attributes of the postprocessor element.
     */
    void setAttributes(Map<String, String> attributeMap);
}
