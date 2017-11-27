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

package org.mycore.access.mcrimpl;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;

/**
 * The purpose of this interface is to make the choice of the persistence layer
 * configurable. Any concrete database-class which stores MyCoRe Access control
 * must implement this interface. Which database actually will be used can then
 * be configured by reading the value <code>MCR.Persistence.Rule.Store_Class</code>
 * from mycore.properties.access
 * 
 * @author Arne Seifert
 */
public abstract class MCRRuleStore {
    private static final Logger LOGGER = LogManager.getLogger(MCRRuleStore.class);

    protected static final String sqlDateformat = "yyyy-MM-dd HH:mm:ss";

    protected static final String ruletablename = MCRConfiguration.instance().getString(
        "MCR.Persistence.Access.Store.Table.Rule",
        "MCRACCESSRULE");

    private static MCRRuleStore implementation;

    public abstract void createRule(MCRAccessRule rule);

    public abstract void updateRule(MCRAccessRule rule);

    public abstract void deleteRule(String ruleid);

    public abstract MCRAccessRule getRule(String ruleid);

    public abstract boolean existsRule(String ruleid);

    public abstract Collection<String> retrieveAllIDs();

    public abstract Collection<String> retrieveRuleIDs(String ruleExpression, String description);

    public abstract int getNextFreeRuleID(String prefix);

    public static MCRRuleStore getInstance() {
        try {
            if (implementation == null) {
                implementation = MCRConfiguration.instance().getSingleInstanceOf(
                    "MCR.Persistence.Rule.Store_Class",
                    "org.mycore.backend.hibernate.MCRHIBRuleStore");
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }

        return implementation;
    }
}
