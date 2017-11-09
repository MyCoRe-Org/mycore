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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jdom2.Element;

import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexEntry;
import bibtex.dom.BibtexString;

class MCRUnsupportedFieldTransformer extends MCRFieldTransformer {

    Set<String> supportedFields = new HashSet<String>();

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

    void transformField(BibtexEntry entry, Element parent) {
        for (String field : (entry.getFields().keySet())) {
            if (isUnsupported(field)) {
                this.field = field;
                super.transformField(entry, parent);
            }
        }
    }

    void buildField(BibtexAbstractValue value, Element parent) {
        MCRMessageLogger.logMessage("Field " + field + " is unsupported: " + value.toString().replaceAll("\\s+", " "));
        String xPath = "mods:extension/field[@name='" + field + "']" + MCRFieldTransformer.AS_NEW_ELEMENT;
        String content = ((BibtexString) value).getContent();
        content = normalizeValue(content);
        buildElement(xPath, content, parent);
    }
}
