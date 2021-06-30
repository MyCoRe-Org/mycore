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

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.xml.MCRFacts;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectService;

public class MCRStatusCondition extends MCRSimpleCondition {
    private static Logger LOGGER = LogManager.getLogger();

    /**
     * id of the fact that contains the ID of the MyCoRe-Object or Derivate
     * possible values are "id" or "derivateid".
     */
    private String idFact = "id";

    @Override
    public void parse(Element xml) {
        super.parse(xml);
        if (xml.getAttributeValue("id") != null) {
            LOGGER.warn("Attribute 'id' is deprecated - use 'fact' instead!");
        }
        this.idFact = Optional.ofNullable(xml.getAttributeValue("fact"))
            .orElse(Optional.ofNullable(xml.getAttributeValue("id")).orElse("id"));
    }

    @Override
    public boolean matches(MCRFacts facts) {
        facts.require(this.type);
        return super.matches(facts);
    }

    @Override
    public void setCurrentValue(MCRFacts facts) {
        MCRIDCondition idc = (MCRIDCondition) (facts.require(idFact));
        MCRObject object = idc.getObject();
        if (object == null) {
            super.setCurrentValue(facts);
        } else {
            MCRObjectService service = object.getService();
            MCRCategoryID status = service.getState();
            if (status != null) {
                value = status.getID();
            }
        }
    }
}
