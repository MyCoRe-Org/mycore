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

import bibtex.dom.BibtexPerson;

/**
 * Transforms a BibTeX person name to a JDOM mods:name element.
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRPersonTransformer extends MCRFieldTransformer {

    protected String xPath;

    MCRPersonTransformer(String field, String role) {
        super(field);
        this.xPath = "mods:name[@type='personal'][mods:role/mods:roleTerm[@type='code'][@authority='marcrelator']='"
                + role + "']" + MCRFieldTransformer.AS_NEW_ELEMENT;
    }

    void buildField(BibtexPerson person, Element parent) {
        Element modsName = buildElement(xPath, null, parent);

        String lastName = normalizeValue(person.getLast());
        buildElement("mods:namePart[@type='family']", lastName, modsName);

        String firstName = getFirstName(person);
        if (!firstName.isEmpty()) {
            buildElement("mods:namePart[@type='given']", firstName, modsName);
        }

        String lineage = person.getLineage();
        if (lineage != null) {
            buildElement("mods:namePart[@type='termsOfAddress']", lineage, modsName);
        }
    }

    private String getFirstName(BibtexPerson person) {
        StringBuffer first = new StringBuffer();
        if (person.getFirst() != null) {
            first.append(person.getFirst());
        }
        if (person.getPreLast() != null) {
            first.append(' ').append(person.getPreLast());
        }
        String firstName = normalizeValue(first.toString().trim());
        return firstName;
    }
}
