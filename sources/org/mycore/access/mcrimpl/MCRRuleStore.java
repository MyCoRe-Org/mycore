/*
 * 
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

package org.mycore.access.mcrimpl;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;

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
    public abstract void createRule(MCRAccessRule rule);

    public abstract void updateRule(MCRAccessRule rule);

    public abstract void deleteRule(String ruleid);

    public abstract MCRAccessRule getRule(String ruleid);

    public abstract boolean existsRule(String ruleid);
    
    public abstract Collection<String> retrieveAllIDs();
    
    public abstract Collection<String> retrieveRuleIDs(String ruleExpression, String description);
    
    public abstract int getNextFreeRuleID(String prefix);

    public abstract MCRAccessRule retrieveRule(String ruleid);

    public static Logger logger = Logger.getLogger(MCRRuleStore.class.getName());

    final protected static String sqlDateformat = "yyyy-MM-dd HH:mm:ss";

    final protected static String ruletablename = MCRConfiguration.instance().getString("MCR.Persistence.Access.Store.Table.Rule", "MCRACCESSRULE");

    static private MCRRuleStore implementation;

    public static MCRRuleStore getInstance() {
        try {
            if (implementation == null) {
                implementation = (MCRRuleStore) MCRConfiguration.instance().getSingleInstanceOf("MCR.Persistence.Rule.Store_Class", "org.mycore.backend.hibernate.MCRHIBRuleStore");
            }
        } catch (Exception e) {
            logger.error(e);
        }

        return implementation;
    }
}
