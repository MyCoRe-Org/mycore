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
package org.mycore.common.xml;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;

/**
 * A jdom-filter which compares attribute values.
 */
public class MCRAttributeValueFilter extends ElementFilter {
    private static final long serialVersionUID = 1L;

    protected String attrKey;

    protected String attrValue;

    protected Namespace ns;

    public MCRAttributeValueFilter(String attrKey, Namespace ns, String attrValue) {
        super();
        this.attrKey = attrKey;
        this.attrValue = attrValue;
        this.ns = ns;
    }

    public Element filter(Object arg0) {
        Element e = super.filter(arg0);
        if (e == null) {
            return null;
        }
        String value = e.getAttributeValue(attrKey, ns);
        return (value != null && value.equals(attrValue)) ? e : null;
    }
}
