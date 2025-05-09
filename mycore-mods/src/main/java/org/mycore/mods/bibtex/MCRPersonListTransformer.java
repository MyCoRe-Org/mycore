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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexPerson;
import bibtex.dom.BibtexPersonList;

/**
 * Transforms a BibTeX field containing author/editor names to multiple JDOM mods:name elements.
 *
 * @author Frank Lützenkirchen
 */
class MCRPersonListTransformer extends MCRFieldTransformer {

    private static final Logger LOGGER = LogManager.getLogger();

    private MCRPersonTransformer personTransformer;

    private MCRAndOthersTransformer andOthers;

    MCRPersonListTransformer(String field, String role) {
        super(field);
        this.personTransformer = new MCRPersonTransformer(field, role);
        this.andOthers = new MCRAndOthersTransformer(field, role);
    }

    @Override
    @SuppressWarnings("unchecked")
    void buildField(BibtexAbstractValue value, Element parent) {
        if (!(value instanceof BibtexPersonList personList)) {
            if (LOGGER.isErrorEnabled()) {// false-positive on PMD:GuardLogStatement (static method reference)
                LOGGER.error("Cannot not cast {} ({}) to {}", BibtexAbstractValue.class::getName, () -> value,
                    BibtexPersonList.class::getName);
            }
            return;
        }
        for (BibtexPerson person : (List<BibtexPerson>) (personList.getList())) {
            (person.isOthers() ? andOthers : personTransformer).buildField(person, parent);
        }
    }
}
