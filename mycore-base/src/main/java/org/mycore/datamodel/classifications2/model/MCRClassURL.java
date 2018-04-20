
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

package org.mycore.datamodel.classifications2.model;

import java.net.URI;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MCRClassURL")
@JsonFormat(shape = JsonFormat.Shape.STRING)
public class MCRClassURL {

    @XmlAttribute(name = "type", namespace = "http://www.w3.org/1999/xlink")
    protected static final String TYPE = "locator";

    @XmlAttribute(name = "href", namespace = "http://www.w3.org/1999/xlink")
    @XmlSchemaType(name = "anyURI")
    protected String href;

    @JsonValue
    public String getHref() {
        return href;
    }

    public void setHref(String value) {
        this.href = value;
    }

    @JsonCreator
    public static MCRClassURL getInstance(URI uri) {
        if (uri == null) {
            return null;
        }
        MCRClassURL url = new MCRClassURL();
        url.setHref(uri.toASCIIString());
        return url;
    }

}
