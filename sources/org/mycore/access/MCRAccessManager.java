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

import java.util.Date;
import java.net.UnknownHostException;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRException;
import org.mycore.user.MCRUserMgr;
import org.mycore.user.MCRUser;

/**
 * Maps object ids to rules
 * 
 * @author   Matthias Kramm
 **/

public class MCRAccessManager
{
    MCRCache cache;
    MCRAccessStore accessStore;
    MCRRuleStore ruleStore;
    MCRAccessRule dummyRule;
    boolean disabled = false;

    public MCRAccessManager()
    {
        MCRConfiguration config = MCRConfiguration.instance();
        int size = config.getInt("MCR.AccessPool.CacheSize", 2048);
        String pools = config.getString("MCR.AccessPools","");
        if(pools.trim().length() == 0)
            disabled = true;
        cache = new MCRCache(size);
        accessStore = MCRAccessStore.getInstance();
        ruleStore = MCRRuleStore.getInstance();

        dummyRule = new MCRAccessRule(null,null,null,null,"dummy rule, always true");
    }

    private static MCRAccessManager singleton;
    public static synchronized MCRAccessManager instance()
    {
        if(singleton==null)
            singleton = new MCRAccessManager();
        return singleton;
    }

    public MCRAccessRule getAccess(String pool, String objID)
    {
        if(disabled)
            return dummyRule;
        MCRAccessRule a = (MCRAccessRule)cache.get(pool + "#" + objID);
        if(a==null) {
            String ruleID = accessStore.getRuleID(objID,pool);
            if(ruleID != null) {
                a = ruleStore.getRule(ruleID);
            } else {
                a = null;
            }
            if(a == null) {
                a = dummyRule;
            }
            cache.put(pool + "#" + objID, a);
        }
        return a;
    }

    public static boolean checkAccess(String pool, String objID, MCRUser user, MCRIPAddress ip)
    {
        Date date = new Date();
        MCRAccessRule rule = instance().getAccess(pool, objID);
        if(rule == null)
            return true; //no rule: everybody can access this
        return rule.checkAccess(user, date, ip);
    }
    public static boolean checkReadAccess(String objID, MCRUser user, MCRIPAddress ip)
    {
        return checkAccess("READ", objID, user, ip);
    }
    public static boolean checkAccess(String pool, String objID, MCRSession session)
    {
	MCRUser user = MCRUserMgr.instance().retrieveUser(session.getCurrentUserID());
        MCRIPAddress ip;
        try {
            ip = new MCRIPAddress(session.getIp());
        } catch(UnknownHostException e) {
            /* this should never happen */
            throw new MCRException("unknown host", e);
        }
        return checkAccess(pool, objID, user, ip);
    }
    public static boolean checkReadAccess(String objID, MCRSession session)
    {
        return checkAccess("READ", objID, session);
    }

};


