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

package org.mycore.mods.access.xml.condition;

import org.jdom2.Element;
import org.mycore.access.xml.MCRFacts;
import org.mycore.access.xml.MCRObjectCacheFactory;
import org.mycore.access.xml.conditions.MCRSimpleCondition;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;

import java.util.List;

public class MCRMODSCollectionCondition extends MCRSimpleCondition {

    private static final String XPATH_COLLECTION = "mods:classification[contains(@valueURI,'/collection#')]";

    @Override
    public boolean matches(MCRFacts facts) {
        facts.require(this.type);
        return super.matches(facts);
    }

    @Override
    public void setCurrentValue(MCRFacts facts) {
        MCRSimpleCondition idc = (MCRSimpleCondition) (facts.require("id"));
        MCRObject object = MCRObjectCacheFactory.instance().getObject(MCRObjectID.getInstance(idc.value));
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);

        List<Element> e = wrapper.getElements(XPATH_COLLECTION);
        if ((e != null) && !(e.isEmpty())) {
            String valueURI = e.get(0).getAttributeValue("valueURI");
            this.value = valueURI.split("#")[1];
        }
    }
}
