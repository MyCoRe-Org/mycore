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
        expand(new MacroReferenceExpander(true, true, true, false), file);
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
