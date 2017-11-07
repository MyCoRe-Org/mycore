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

import java.util.List;

import org.jdom2.Element;

import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexPerson;
import bibtex.dom.BibtexPersonList;

class MCRPersonListTransformer extends MCRFieldTransformer {

    private MCRPersonTransformer personTransformer;

    private MCRAndOthersTransformer andOthers;

    MCRPersonListTransformer(String field, String role) {
        super(field);
        this.personTransformer = new MCRPersonTransformer(field, role);
        this.andOthers = new MCRAndOthersTransformer(field, role);
    }

    @SuppressWarnings("unchecked")
    void buildField(BibtexAbstractValue value, Element parent) {
        BibtexPersonList personList = (BibtexPersonList) value;
        for (BibtexPerson person : (List<BibtexPerson>) (personList.getList())) {
            (person.isOthers() ? andOthers : personTransformer).buildField(person, parent);
        }
    }
}
