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

package org.mycore.common.xsl.uriresolver;

import javax.xml.transform.Source;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;

/**
 * Utility methods for creating standard XML responses in MyCoRe URI resolvers.
 *
 * <p>All methods return a {@link Source} wrapping a simple JDOM element,
 * which can be returned directly from a {@link javax.xml.transform.URIResolver}.
 */
public final class MCRURIResolverResponse {

    /**
     * Element name used to represent a {@code null} response.
     *
     * @see #ofNull()
     */
    private static final String ELEMENT_NULL = "null";

    private MCRURIResolverResponse() {
    }

    /**
     * Creates a boolean XML response of the form:
     * <pre>{@code <boolean>true|false</boolean>}</pre>
     *
     * @param value the boolean value to encode
     * @return a {@link Source} wrapping the response element
     */
    public static Source ofBoolean(boolean value) {
        Element root = new Element("boolean");
        root.setText(Boolean.toString(value));
        return new JDOMSource(root);
    }

    /**
     * Creates a string XML response of the form:
     * <pre>{@code <string>text</string>}</pre>
     *
     * @param text the text to encode; may be empty but not {@code null}
     * @return a {@link Source} wrapping the response element
     */
    public static Source ofString(String text) {
        Element root = new Element("string");
        root.setText(text);
        return new JDOMSource(root);
    }

    /**
     * Creates a null XML response of the form:
     * <pre>{@code <null/>}</pre>
     *
     * <p>Used to signal the absence of a result in contexts where
     * returning Java {@code null} is not appropriate.
     *
     * @return a {@link Source} wrapping the null element
     */
    public static Source ofNull() {
        return new JDOMSource(new Element(ELEMENT_NULL));
    }

}
