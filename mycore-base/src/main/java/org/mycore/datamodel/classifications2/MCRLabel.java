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

package org.mycore.datamodel.classifications2;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * This class represents a label of a MCRCategory.
 * 
 * @author Thomas Scheffler (yagee)
 * @since 2.0
 */
@XmlRootElement(
    name = "label")
@XmlAccessorType(XmlAccessType.FIELD)
@Embeddable
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class MCRLabel implements Cloneable, Serializable, Comparable<MCRLabel> {

    private static final long serialVersionUID = -843799854929361194L;

    @XmlAttribute(
        namespace = "http://www.w3.org/XML/1998/namespace")
    String lang;

    @XmlAttribute
    String text;

    @XmlAttribute
    String description;

    public MCRLabel() {

    }

    /**
     * @param lang see {@link #setLang(String)}
     * @param text see {@link #setText(String)}
     * @param description see {@link #setDescription(String)}
     * @throws NullPointerException if lang or text is null
     * @throws IllegalArgumentException if lang or text is invalid
     */
    public MCRLabel(String lang, String text, String description)
        throws NullPointerException, IllegalArgumentException {
        super();
        setLang(lang);
        setText(text);
        setDescription(description);
    }

    @Column
    public String getLang() {
        return lang;
    }

    /**
     * @param lang language tag in RFC4646 form
     * @throws NullPointerException if lang is null
     * @throws IllegalArgumentException if lang is somehow invalid (empty or 'und')
     */
    public void setLang(String lang) {
        Objects.requireNonNull(lang, "'lang' of label may not be null.");
        if (lang.trim().isEmpty()) {
            throw new IllegalArgumentException("'lang' of label may not be empty.");
        }
        Locale locale = Locale.forLanguageTag(lang);
        String languageTag = locale.toLanguageTag();
        if (Objects.equals(languageTag, "und")) {
            throw new IllegalArgumentException("'lang' of label is not valid language tag (RFC4646):" + lang);
        }
        this.lang = languageTag;
    }

    @Column(length = 4096)
    public String getText() {
        return text;
    }

    /**
     * @param text required attribute of label
     * @throws NullPointerException if text is null
     * @throws IllegalArgumentException if text is empty
     */
    public void setText(String text) {
        Objects.requireNonNull(text, "'text' of label('" + lang + "') may not be null.");
        if (text.trim().isEmpty()) {
            throw new IllegalArgumentException("'text' of label('" + lang + "') may not be empty.");
        }
        this.text = text;
    }

    @Column(length = 4096)
    public String getDescription() {
        if (description == null) {
            return "";
        }
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public MCRLabel clone() {
        MCRLabel clone = null;
        try {
            clone = (MCRLabel) super.clone();
        } catch (CloneNotSupportedException ce) {
            // Can not happen
        }
        return clone;
    }

    @Override
    public String toString() {
        return getLang() + '(' + getText() + ')';
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getDescription().hashCode();
        result = prime * result + (lang == null ? 0 : lang.hashCode());
        result = prime * result + (text == null ? 0 : text.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MCRLabel other = (MCRLabel) obj;
        if (lang == null) {
            if (other.lang != null) {
                return false;
            }
        } else if (!lang.equals(other.lang)) {
            return false;
        }
        if (text == null) {
            if (other.text != null) {
                return false;
            }
        } else if (!text.equals(other.text)) {
            return false;
        }
        return getDescription().equals(other.getDescription());
    }

    @Override
    public int compareTo(MCRLabel other) {
            if (other == null) {
                return 1;
            }
            //both are not null
            if (this.getLang() == other.getLang()) { // this intentionally uses == to allow for both null
                return 0;
            }
            if (this.getLang() == null) {
                return -1;
            }
            if (other.getLang() == null) {
                return 1;
            }
            return this.getLang().compareTo(other.getLang());
    }
}
