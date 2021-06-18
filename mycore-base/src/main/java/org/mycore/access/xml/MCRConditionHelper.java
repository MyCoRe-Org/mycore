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
package org.mycore.access.xml;

import org.jdom2.Element;
import org.mycore.access.xml.conditions.MCRCombinedCondition;
import org.mycore.access.xml.conditions.MCRCondition;
import org.mycore.access.xml.conditions.MCRNotCondition;
import org.mycore.access.xml.conditions.MCRSimpleCondition;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

import java.util.Optional;

public class MCRConditionHelper {

    public static final String CONDITION_PREFIX = "MCR.Access.XML.";

    public static MCRCondition parse(Element xml) {
        String type = xml.getName();
        MCRCondition cond = build(type);
        cond.parse(xml);
        return cond;
    }

    public static MCRSimpleCondition build(String type, String value) {
        MCRSimpleCondition condition = (MCRSimpleCondition) (build(type));
        condition.setValue(value);
        return condition;
    }

    static MCRCondition build(String type) {
        Optional<MCRCondition> conditionOpt = MCRConfiguration2.getInstanceOf(CONDITION_PREFIX + type);

        if (conditionOpt.isEmpty()) {
            throw new MCRConfigurationException("The Condition type " + type + " is not configured!");
        }

        MCRCondition condition = conditionOpt.get();

        if (condition instanceof MCRSimpleCondition) {
            ((MCRSimpleCondition) condition).setType(type);
        }

        if(MCRXMLAccessSystem.LOGGER.isDebugEnabled()){
            if(condition instanceof MCRCombinedCondition){
                ((MCRCombinedCondition) condition).setDebug(true);
            }
            if(condition instanceof MCRNotCondition){
                ((MCRNotCondition)condition).setDebug(true);
            }
        }

        return condition;
    }
}
