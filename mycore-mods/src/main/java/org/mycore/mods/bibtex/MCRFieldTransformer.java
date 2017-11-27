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

package org.mycore.mods.bibtex;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRNodeBuilder;

import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexEntry;

/**
 * Transforms a single BibTeX field to a MODS element.
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRFieldTransformer {

    static final String AS_NEW_ELEMENT = "[999]";

    protected String field;

    MCRFieldTransformer(String field) {
        this.field = field;
    }

    String getField() {
        return field;
    }

    /** Converts german umlauts and other special LaTeX characters */
    protected String normalizeValue(String value) {
        return value.replaceAll("\\s+", " ").trim().replace("{\\\"a}", "\u00e4").replace("{\\\"o}", "\u00f6")
            .replace("{\\\"u}", "\u00fc").replace("{\\\"A}", "\u00c4").replace("{\\\"O}", "\u00d6")
            .replace("{\\\"U}", "\u00dc").replace("{\\ss}", "\u00df").replace("\\\"a", "\u00e4")
            .replace("\\\"o", "\u00f6").replace("\\\"u", "\u00fc").replace("\\\"A", "\u00c4")
            .replace("\\\"O", "\u00d6").replace("\\\"U", "\u00dc").replace("{\\'a}", "\u00e1")
            .replace("{\\'e}", "\u00e9").replace("{\\'i}", "\u00ed").replace("{\\'o}", "\u00f3")
            .replace("{\\'u}", "\u00fa").replace("{\\`a}", "\u00e0").replace("{\\`e}", "\u00e8")
            .replace("{\\`i}", "\u00ec").replace("{\\`o}", "\u00f2").replace("{\\`u}", "\u00f9")
            .replace("{\\'\\i}", "\u00ed").replace("{\\`\\i}", "\u00ec").replace("{", "").replace("}", "")
            .replace("---", "-").replace("--", "-");
    }

    void transformField(BibtexEntry entry, Element parent) {
        for (BibtexAbstractValue value : getFieldValues(entry, field)) {
            buildField(value, parent);
        }
    }

    private List<BibtexAbstractValue> getFieldValues(BibtexEntry entry, String field) {
        List<BibtexAbstractValue> fieldValues = entry.getFieldValuesAsList(field);
        if (fieldValues == null) {
            fieldValues = entry.getFieldValuesAsList(field.toUpperCase(Locale.ROOT));
        }
        return fieldValues == null ? Collections.emptyList() : fieldValues;
    }

    void buildField(BibtexAbstractValue value, Element parent) {
        String type = value.getClass().getSimpleName();
        MCRMessageLogger.logMessage("Field " + field + " returns unsupported abstract value of type " + type, parent);
    }

    static Element buildElement(String xPath, String content, Element parent) {
        try {
            return new MCRNodeBuilder().buildElement(xPath, content, parent);
        } catch (JaxenException ex) {
            throw new MCRException("Unable to build field " + xPath, ex);
        }
    }
}
