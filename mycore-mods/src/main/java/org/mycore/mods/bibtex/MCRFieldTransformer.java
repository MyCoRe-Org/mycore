/*
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
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

class MCRFieldTransformer {

    protected String field;

    MCRFieldTransformer(String field) {
        this.field = field;
    }

    String getField() {
        return field;
    }

    final static String AS_NEW_ELEMENT = "[999]";

    /** Converts german umlauts and other special LaTeX characters */
    protected String normalizeValue(String value) {
        value = value.replaceAll("\\s+", " ").trim();

        value = value.replace("{\\\"a}", "\u00e4");
        value = value.replace("{\\\"o}", "\u00f6");
        value = value.replace("{\\\"u}", "\u00fc");
        value = value.replace("{\\\"A}", "\u00c4");
        value = value.replace("{\\\"O}", "\u00d6");
        value = value.replace("{\\\"U}", "\u00dc");
        value = value.replace("{\\ss}", "\u00df");
        value = value.replace("\\\"a", "\u00e4");
        value = value.replace("\\\"o", "\u00f6");
        value = value.replace("\\\"u", "\u00fc");
        value = value.replace("\\\"A", "\u00c4");
        value = value.replace("\\\"O", "\u00d6");
        value = value.replace("\\\"U", "\u00dc");
        value = value.replace("{\\'a}", "\u00e1");
        value = value.replace("{\\'e}", "\u00e9");
        value = value.replace("{\\'i}", "\u00ed");
        value = value.replace("{\\'o}", "\u00f3");
        value = value.replace("{\\'u}", "\u00fa");
        value = value.replace("{\\`a}", "\u00e0");
        value = value.replace("{\\`e}", "\u00e8");
        value = value.replace("{\\`i}", "\u00ec");
        value = value.replace("{\\`o}", "\u00f2");
        value = value.replace("{\\`u}", "\u00f9");
        value = value.replace("{\\'\\i}", "\u00ed");
        value = value.replace("{\\`\\i}", "\u00ec");

        value = value.replace("{", "").replace("}", "");
        value = value.replace("---", "-").replace("--", "-");
        return value;
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
        return fieldValues == null ? Collections.<BibtexAbstractValue>emptyList() : fieldValues;
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