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

import java.io.IOException;
import java.io.StringReader;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRException;

/**
 * Helper class to encode and decode JDOM Elements as Strings.
 * The change tracker uses String representations of removed/changed elements
 * to avoid object references to the JDOM nodes and simplify cloning. 
 */
public class MCRElementEncoder {

    private static final XMLOutputter RAW_OUTPUTTER = new XMLOutputter(Format.getRawFormat().setEncoding("UTF-8"));

    protected static String element2text(Element element) {
        return RAW_OUTPUTTER.outputString(element);
    }

    protected static Element text2element(String text) {
        try {
            return new SAXBuilder().build(new StringReader(text)).detachRootElement();
        } catch (JDOMException | IOException ex) {
            throw new MCRException("Exception in text2element: " + text, ex);
        }
    }
}
