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
package org.mycore.access.facts;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.facts.model.MCRCombinedCondition;
import org.mycore.access.facts.model.MCRCondition;
import org.mycore.common.config.MCRConfiguration2;

/**
 * Utility functions for parsing conditions from rules.xml
 * in fact-based access system.
 * 
 * @author Robert Stephan
 *
 */
public class MCRFactsAccessSystemHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String CONDITION_PREFIX = "MCR.Access.Facts.Condition.";

    public static MCRCondition parse(Element xml) {
        String type = xml.getName();
        MCRCondition cond = build(type);
        cond.parse(xml);
        return cond;
    }

    static MCRCondition build(String type) {
        MCRCondition condition = MCRConfiguration2.getInstanceOfOrThrow(MCRCondition.class, CONDITION_PREFIX + type);
        if (LOGGER.isDebugEnabled() && condition instanceof MCRCombinedCondition combCond) {
            combCond.setDebug(true);
        }
        return condition;
    }

}
