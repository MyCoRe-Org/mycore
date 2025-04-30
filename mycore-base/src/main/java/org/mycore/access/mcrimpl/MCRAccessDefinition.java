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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps object ids to rules
 *
 * @author Arne Seifert
 */
public class MCRAccessDefinition {

    private String objid;

    private Map<String, String> pools = new ConcurrentHashMap<>();

    public MCRAccessDefinition() {
        pools.clear();
    }

    public String getObjID() {
        return objid;
    }

    public void setObjID(String value) {
        objid = value;
    }

    public Map<String, String> getPool() {
        return pools;
    }

    public void setPool(Map<String, String> pool) {
        pools = pool;
    }

    public void addPool(String poolname, String ruleid) {
        pools.put(poolname, ruleid);
    }

    public void clearPools() {
        pools.clear();
    }
}
