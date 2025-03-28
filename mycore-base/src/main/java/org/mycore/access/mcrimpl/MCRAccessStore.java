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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.common.MCRXMLMetadataManager;

/**
 * The purpose of this class is to make the choice of the persistence layer
 * configurable. Any concrete database-class which stores MyCoRe Access control
 * must implement this interface. Which database actually will be used can then
 * be configured by reading the value <code>MCR.Persistence.Access.Store.Class</code>
 * from mycore.properties.access
 *
 * @author Arne Seifert
 */
public abstract class MCRAccessStore {
    private static final Logger LOGGER = LogManager.getLogger();

    protected static final String SQL_DATEFORMAT = "yyyy-MM-dd HH:mm:ss";

    protected static final String ACCESS_POOLS = MCRConfiguration2.getString("MCR.AccessPools")
        .orElse("read,write,delete");

    public abstract String getRuleID(String objID, String acPool);

    public abstract void createAccessDefinition(MCRRuleMapping accessdata);

    public abstract void deleteAccessDefinition(MCRRuleMapping accessdata);

    public abstract void updateAccessDefinition(MCRRuleMapping accessdata);

    public abstract MCRRuleMapping getAccessDefinition(String pool, String objid);

    public abstract Collection<String> getMappedObjectId(String pool); // ArrayList with ObjID's as String

    public abstract Collection<String> getPoolsForObject(String objid); // ArrayList with pools as String

    public abstract Collection<String> getDatabasePools();

    public abstract boolean existsRule(String objid, String pool);

    public abstract boolean isRuleInUse(String ruleid);

    /**
     *
     * @return a collection of all String IDs an access rule is assigned to
     */
    public abstract Collection<String> getDistinctStringIDs();

    /**
     * @deprecated Use {@link #obtainInstance()} instead
     */
    @Deprecated
    public static MCRAccessStore getInstance() {
        return obtainInstance();
    }

    public static MCRAccessStore obtainInstance() {
        return LazyInstanceHolder.SHARED_INSTANCE;
    }

    public static Collection<String> getPools() {
        String[] pool = ACCESS_POOLS.split(",");
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
            Map<String, Collection<String>> sqlDefinition = new HashMap<>();
            Collection<String> pools = obtainInstance().getDatabasePools();
            //merge pools
            pools.removeAll(getPools());
            pools.addAll(getPools());

            for (String pool : pools) {
                sqlDefinition.put(pool, obtainInstance().getMappedObjectId(pool));
            }

            Collection<MCRAccessDefinition> ret = new ArrayList<>();
            Collection<String> elements;

            if (MCRConfiguration2.getOrThrow("MCR.Metadata.Type." + type, Boolean::parseBoolean)) {
                elements = MCRXMLMetadataManager.getInstance().listIDsOfType(type);
            } else {
                return Collections.emptySet();
            }

            for (String element : elements) {
                MCRAccessDefinition def = new MCRAccessDefinition();
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
            LOGGER.error("definition loading failed: ", e);
            return null;
        }
    }

    public Collection<MCRAccessDefinition> getRules(String objid) {
        try {
            Collection<String> pools = obtainInstance().getDatabasePools();
            //merge pools
            pools.removeAll(getPools());
            pools.addAll(getPools());

            MCRAccessDefinition def = new MCRAccessDefinition();
            def.setObjID(objid);
            for (String pool : pools) {
                String rule = getRuleID(objid, pool);
                def.addPool(pool, Objects.requireNonNullElse(rule, " "));
            }
            return List.of(def);
        } catch (Exception e) {
            LOGGER.error("definition loading failed: ");
            return null;
        }
    }

    private static final class LazyInstanceHolder {
        public static final MCRAccessStore SHARED_INSTANCE = MCRConfiguration2.getInstanceOfOrThrow(
            MCRAccessStore.class, "MCR.Persistence.Access.Store.Class");
    }

}
