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

import org.jdom2.Element;
import org.mycore.common.MCRConstants;

import bibtex.dom.BibtexEntry;
import bibtex.dom.BibtexFile;
import bibtex.expansions.CrossReferenceExpander;
import bibtex.expansions.Expander;
import bibtex.expansions.ExpansionException;
import bibtex.expansions.MacroReferenceExpander;
import bibtex.expansions.PersonListExpander;

/**
 * Transforms a BibTeX file to a JDOM mods:modsCollection.
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRBibTeXFileTransformer {

    private Element collection = new Element("modsCollection", MCRConstants.MODS_NAMESPACE);

    Element transform(BibtexFile file) {
        expandReferences(file);

        MCRBibTeXEntryTransformer transformer = new MCRBibTeXEntryTransformer();

        for (Object obj : file.getEntries()) {
            if (obj instanceof BibtexEntry) {
                BibtexEntry entry = (BibtexEntry) obj;
                if (entry.getFields().isEmpty()) {
                    MCRMessageLogger.logMessage("Skipping entry of type " + entry.getEntryType() + ", has no fields",
                        collection);
                } else {
                    collection.addContent(transformer.transform(entry));
                }
            }
        }

        return collection;
    }

    private void expandReferences(BibtexFile file) {
        expand(new MacroReferenceExpander(true, true, false, false), file);
        expand(new CrossReferenceExpander(false), file);
        expand(new PersonListExpander(true, true, false), file);
    }

    private void expand(Expander expander, BibtexFile file) {
        try {
            expander.expand(file);
        } catch (ExpansionException ex) {
            MCRMessageLogger.logMessage(ex.toString(), collection);
        }
        for (Exception ex : expander.getExceptions()) {
            MCRMessageLogger.logMessage(ex.toString(), collection);
        }
    }
}
