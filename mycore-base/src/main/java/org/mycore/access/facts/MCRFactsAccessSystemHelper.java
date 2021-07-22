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
package org.mycore.access.facts;

import java.util.Optional;

import org.jdom2.Element;
import org.mycore.access.facts.model.MCRCombinedCondition;
import org.mycore.access.facts.model.MCRCondition;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

/**
 * Utility functions for parsing conditions from rules.xml
 * in fact-based access system.
 * 
 * @author Robert Stephan
 *
 */
public class MCRFactsAccessSystemHelper {

    public static final String CONDITION_PREFIX = "MCR.Access.Facts.Condition.";

    public static MCRCondition parse(Element xml) {
        String type = xml.getName();
        MCRCondition cond = build(type);
        cond.parse(xml);
        return cond;
    }

    static MCRCondition build(String type) {
        Optional<MCRCondition> optCondition = MCRConfiguration2.getInstanceOf(CONDITION_PREFIX + type);
        if (optCondition.isEmpty()) {
            throw new MCRConfigurationException("The Condition type " + type + " is not configured!");
        }

        MCRCondition condition = optCondition.get();
        if (MCRFactsAccessSystem.LOGGER.isDebugEnabled()
            && condition instanceof MCRCombinedCondition) {
            ((MCRCombinedCondition) condition).setDebug(true);
        }

        return condition;
    }
}
