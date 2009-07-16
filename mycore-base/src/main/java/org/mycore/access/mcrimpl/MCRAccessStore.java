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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.common.MCRXMLTableManager;

/**
 * The purpose of this interface is to make the choice of the persistence layer
 * configurable. Any concrete database-class which stores MyCoRe Access control
 * must implement this interface. Which database actually will be used can then
 * be configured by reading the value <code>MCR.Persistence.Access.Store.Class</code>
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

    public abstract MCRRuleMapping getAccessDefinition(String pool, String objid);

    public abstract Collection<String> getMappedObjectId(String pool); // ArrayList with ObjID's as String

    public abstract Collection<String> getPoolsForObject(String objid); // ArrayList with pools as String

    public abstract Collection<String> getDatabasePools();

    public abstract boolean existsRule(String objid, String pool);

    /**
     * 
     * @return a collection of all String IDs an access rule is assigned to
     */
    public abstract Collection<String> getDistinctStringIDs();

    public static Logger logger = Logger.getLogger(MCRAccessStore.class.getName());

    final protected static String sqlDateformat = "yyyy-MM-dd HH:mm:ss";

    final protected static String SQLAccessCtrlRule = MCRConfiguration.instance().getString("MCR.Persistence.Access.Store.Table.Rule", "MCRACCESSRULE");

    final protected static String SQLAccessCtrlMapping = MCRConfiguration.instance().getString("MCR.Persistence.Access.Store.Table.Map", "MCRACCESS");

    final protected static String AccessPools = MCRConfiguration.instance().getString("MCR.AccessPools", "read,write,delete");

    static private MCRAccessStore implementation;

    public static MCRAccessStore getInstance() {
        try {
            if (implementation == null) {
                implementation = (MCRAccessStore) MCRConfiguration.instance().getSingleInstanceOf("MCR.Persistence.Access.Store.Class",
                        "org.mycore.backend.hibernate.MCRHIBAccessStore");
            }
        } catch (Exception e) {
            logger.error(e);
        }

        return implementation;
    }

    public static Collection<String> getPools() {
        String[] pool = AccessPools.split(",");
        return Arrays.asList(pool);
    }

    /**
     * alle Elemente eines Datentypes aufbereiten
     * @param type document type
     * 
     * @return List of MCRAccessDefinition
     * @see MCRAccessDefinition
     */
    public Collection<MCRAccessDefinition> getDefinition(String type) {
        try {
            HashMap<String, Collection<String>> sqlDefinition = new HashMap<String, Collection<String>>();
            Collection<String> pools = MCRAccessStore.getInstance().getDatabasePools();
            //merge pools
            pools.removeAll(getPools());
            pools.addAll(getPools());

            for (String pool : pools) {
                sqlDefinition.put(pool, MCRAccessStore.getInstance().getMappedObjectId(pool));
            }

            Collection<MCRAccessDefinition> ret = new LinkedList<MCRAccessDefinition>();
            Collection<String> elements;
            MCRAccessDefinition def = null;

            if (MCRConfiguration.instance().getBoolean("MCR.Metadata.Type." + type)) {
                elements = MCRXMLTableManager.instance().retrieveAllIDs(type);
            } else
                return Collections.emptySet();

            for (String element : elements) {
                def = new MCRAccessDefinition();
                def.setObjID(element);
                for (String pool : pools) {
                    Collection<String> l = sqlDefinition.get(pool);
                    if (l.contains(element)) {
                        def.addPool(pool, "X");
                    } else {
                        def.addPool(pool, " ");
                    }
                }
                ret.add(def);
            }
            return ret;
        } catch (Exception e) {
            logger.error("definition loading failed: ", e);
            return null;
        }
    }

    public Collection<MCRAccessDefinition> getRules(String objid) {
        try {
            Collection<String> pools = MCRAccessStore.getInstance().getDatabasePools();
            //merge pools
            pools.removeAll(getPools());
            pools.addAll(getPools());

            Collection<MCRAccessDefinition> ret = new LinkedList<MCRAccessDefinition>();
            //List elements = new LinkedList();
            MCRAccessDefinition def = new MCRAccessDefinition();
            def.setObjID(objid);
            for (String pool : pools) {
                String rule = getRuleID(objid, pool);
                if (rule != null) {
                    def.addPool(pool, rule);
                } else {
                    def.addPool(pool, " ");
                }
            }
            ret.add(def);
            return ret;
        } catch (Exception e) {
            logger.error("definition loading failed: ");
            return null;
        }
    }

}
