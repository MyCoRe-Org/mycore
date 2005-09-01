/**
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 **/

package org.mycore.access;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;

/**
 * Maps object ids to rules
 * 
 * @author   Matthias Kramm
 **/

public class MCRAccessManager
{
    MCRRuleStore store;
    MCRCache cache;

    public MCRAccessManager()
    {
        MCRConfiguration config = MCRConfiguration.instance();
        int size = config.getInt("MCR.AccessPool.CacheSize", 2048);
        cache = new MCRCache(size);
        store = MCRRuleStore.getInstance();
    }

    private MCRRuleStore singleton;
    public synchronized MCRRuleCache instance()
    {
        if(singleton==null)
            singleton = new MCRRuleCache();
        return singleton;
    }

    public MCRAccessRule getAccess(String id)
    {
        MCRAccessRule a = (MCRAccessRule)cache.get(id);
        if(a==null) {
            a = store.getRule(id);
            cache.put(id, a);
        }
        return a;
    }

    public static boolean checkAccess(String objid, MCRUser user, MCRIPAddress ip)
    {
        Date date = new Date();
        MCRAccessRule rule = instance().getAccess(objid);
        if(rule == null)
            return true; //no rule: everybody can access this
        return rule.checkAccess(user, date, ip)
    }
};

