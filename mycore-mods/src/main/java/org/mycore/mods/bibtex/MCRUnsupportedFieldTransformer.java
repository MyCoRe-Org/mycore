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

package org.mycore.mods.bibtex;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jdom2.Element;

import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexEntry;
import bibtex.dom.BibtexString;

/**
 * Transforms any BibTeX field that can not be mapped to MODS to a mods:extension/field element.
 *
 * @author Frank Lützenkirchen
 */
class MCRUnsupportedFieldTransformer extends MCRFieldTransformer {

    Set<String> supportedFields = new HashSet<>();

    MCRUnsupportedFieldTransformer(Collection<MCRFieldTransformer> supportedTransformers) {
        super("*");
        determineSupportedFields(supportedTransformers);
    }

    private void determineSupportedFields(Collection<MCRFieldTransformer> supportedTransformers) {
        for (MCRFieldTransformer transformer : supportedTransformers) {
            supportedFields.add(transformer.getField());
        }
    }

    private boolean isUnsupported(String field) {
        return !supportedFields.contains(field);
    }

    @Override
    void transformField(BibtexEntry entry, Element parent) {
        for (String field : (entry.getFields().keySet())) {
            if (isUnsupported(field)) {
                this.field = field;
                super.transformField(entry, parent);
            }
        }
    }

    @Override
    void buildField(BibtexAbstractValue value, Element parent) {
        MCRMessageLogger.logMessage("Field " + field + " is unsupported: " + value.toString().replaceAll("\\s+", " "));
        String xPath = "mods:extension/field[@name='" + field + "']" + AS_NEW_ELEMENT;
        String content = ((BibtexString) value).getContent();
        content = normalizeValue(content);
        buildElement(xPath, content, parent);
    }
}
