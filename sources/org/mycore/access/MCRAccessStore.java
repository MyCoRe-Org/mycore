/*
 * $RCSfile$
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

package org.mycore.access;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;

/**
 * The purpose of this interface is to make the choice of the persistence layer
 * configurable. Any concrete database-class which stores MyCoRe Access control
 * must implement this interface. Which database actually will be used can then
 * be configured by reading the value <code>MCR.accessstore_class_name</code>
 * from mycore.properties.access
 * 
 * @author Arne Seifert
 * @version $Revision$ $Date$
 */
public abstract class MCRAccessStore {
    public abstract void createTables();

    public abstract String getRuleID(String objID, String ACPool);

    public abstract void createAccessDefinition(MCRRuleMapping accessdata);

    public abstract void deleteAccessDefinition(MCRRuleMapping accessdata);

    public abstract void updateAccessDefinition(MCRRuleMapping accessdata);

    public abstract MCRRuleMapping getAccessDefinition(String ruleid, String pool, String objid);

    public static Logger logger = Logger.getLogger(MCRAccessStore.class.getName());

    final protected static String sqlDateformat = "yyyy-MM-dd HH:mm:ss";

    final protected static String SQLAccessCtrlRule = MCRConfiguration.instance().getString("MCR.access_store_sql_table_rule", "MCRACCESSRULE");

    final protected static String SQLAccessCtrlMapping = MCRConfiguration.instance().getString("MCR.access_store_sql_table_map", "MCRACCESS");

    final protected static String AccessPools = MCRConfiguration.instance().getString("MCR.AccessPools", "");

    static private MCRAccessStore implementation;

    public static MCRAccessStore getInstance() {
        try {
            if (implementation == null) {
                implementation = (MCRAccessStore) MCRConfiguration.instance().getSingleInstanceOf("MCR.accessstore_class_name", "org.mycore.backend.sql.MCRSQLAccessStore");
            }
        } catch (Exception e) {
            logger.error(e);
        }

        return implementation;
    }
}
