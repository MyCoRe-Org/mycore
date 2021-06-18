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

package org.mycore.access.xml.conditions;

import org.jdom2.Element;
import org.mycore.access.xml.MCRFacts;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObjectID;

import java.util.Objects;

public class MCRCategoryCondition extends MCRSimpleCondition {

    private String idFact;

    public MCRCategoryCondition(){
        super();
        idFact = "id";
    }

    @Override
    public void parse(Element xml) {
        super.parse(xml);
        this.idFact = xml.getAttributeValue("id");
    }

    @Override
    public boolean matches(MCRFacts facts) {
        MCRCategoryID categoryID = MCRCategoryID.fromString(this.value);

        if (!MCRCategoryDAOFactory.getInstance().exist(categoryID)) {
            return false;
        }

        if (!super.matches(facts)) {
            MCRIDCondition idc = (MCRIDCondition) (facts.require(idFact));
            MCRObjectID objectID = idc.getObjectID();
            if (objectID != null) {
                MCRCategLinkService linkService = MCRCategLinkServiceFactory.getInstance();
                return linkService.isInCategory(new MCRCategLinkReference(objectID), categoryID);
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        MCRCategoryCondition that = (MCRCategoryCondition) o;
        return idFact.equals(that.idFact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), idFact);
    }
}
