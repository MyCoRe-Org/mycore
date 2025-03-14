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

package org.mycore.access.mcrimpl;

import java.util.Collection;

import org.mycore.common.config.MCRConfiguration2;

/**
 * The purpose of this interface is to make the choice of the persistence layer
 * configurable. Any concrete database-class which stores MyCoRe Access control
 * must implement this interface. Which database actually will be used can then
 * be configured by reading the value <code>MCR.Persistence.Rule.Store.Class</code>
 * from mycore.properties.access
 * 
 * @author Arne Seifert
 */
public abstract class MCRRuleStore {

    protected static final String SQL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public abstract void createRule(MCRAccessRule rule);

    public abstract void updateRule(MCRAccessRule rule);

    public abstract void deleteRule(String ruleid);

    public abstract MCRAccessRule getRule(String ruleid);

    public abstract boolean existsRule(String ruleid);

    public abstract Collection<String> retrieveAllIDs();

    public abstract Collection<String> retrieveRuleIDs(String ruleExpression, String description);

    public abstract int getNextFreeRuleID(String prefix);

    /**
     * @deprecated Use {@link #obtainInstance()} instead
     */
    @Deprecated
    public static MCRRuleStore getInstance() {
        return obtainInstance();
    }

    public static MCRRuleStore obtainInstance() {
        return LazyInstanceHolder.SHARED_INSTANCE;
    }

    private static final class LazyInstanceHolder {
        public static final MCRRuleStore SHARED_INSTANCE = MCRConfiguration2.getInstanceOfOrThrow(
            MCRRuleStore.class, "MCR.Persistence.Rule.Store.Class");
    }

}
