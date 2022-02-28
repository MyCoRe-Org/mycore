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

package org.mycore.user2;

import java.util.Comparator;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@Embeddable
@Table(name = "MCRUserAttr",
    indexes = { @Index(name = "MCRUserAttributes", columnList = "name, value"),
        @Index(name = "MCRUserValues", columnList = "value") })
@XmlRootElement(name = "attribute")
public class MCRUserAttribute implements Comparable<MCRUserAttribute> {

    private String name;

    private String value;

    public static final Comparator<MCRUserAttribute> NATURAL_ORDER_COMPARATOR = Comparator
        .comparing(MCRUserAttribute::getName)
        .thenComparing(MCRUserAttribute::getValue);

    protected MCRUserAttribute() {
    }

    public MCRUserAttribute(String name, String value) {
        setName(name);
        setValue(value);
    }

    @Column(name = "name", length = 128, nullable = false)
    @XmlAttribute
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Column(name = "value", length = 255, nullable = false)
    @XmlAttribute
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        //require non-null MCR-2374
        this.value = Objects.requireNonNull(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MCRUserAttribute)) {
            return false;
        }
        MCRUserAttribute that = (MCRUserAttribute) o;
        return name.equals(that.name) &&
            Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public int compareTo(MCRUserAttribute o) {
        return NATURAL_ORDER_COMPARATOR.compare(this, o);
    }
}
