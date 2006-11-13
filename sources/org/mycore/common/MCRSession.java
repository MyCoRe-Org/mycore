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

package org.mycore.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.mapping.Collection;
import org.mycore.datamodel.classifications.MCRClassificationBrowserData;

/**
 * Instances of this class collect information kept during a session like the
 * currently active user, the preferred language etc.
 * 
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @author Frank L�tzenkirchen
 * 
 * @version $Revision$ $Date$
 */
public class MCRSession implements Cloneable {
    /** A map storing arbitrary session data * */
    private Map map = new HashMap();

    /** the logger */
    static Logger logger = Logger.getLogger(MCRSession.class.getName());

    /** The user ID of the session */
    private String userID = null;

    /** The language for this session as upper case character */
    private String language = null;

    /** The unique ID of this session */
    private String sessionID = null;

    /** A cache of MCRSession objects, used for method getSession( String ) */
    private static MCRCache sessions = new MCRCache(1000);

    /** -ASC- f�r MCRClassificationBrowser Class session daten */
    public MCRClassificationBrowserData BData = null;

    private String FullName = null;

    private String CurrentDocumentID = null;

    private String ip = null;
    
    private long loginTime;

    /**
     * The constructor of a MCRSession. As default the user ID is set to the
     * value of the property variable named 'MCR.users_guestuser_username'.
     */
    public MCRSession() {
        MCRConfiguration config = MCRConfiguration.instance();
        userID = config.getString("MCR.users_guestuser_username", "gast");
        language = config.getString("MCR.metadata_default_lang", "de");

        ip = "";
        sessionID = buildSessionID();
        sessions.put(sessionID, this);

        logger.debug("MCRSession created " + sessionID);
        setLoginTime();
    }
    
    public final void setLoginTime(){
        loginTime=System.currentTimeMillis();
    }

    /**
     * Returns the MCRSession for the given sessionID.
     */
    public static MCRSession getSession(String sessionID) {
        MCRSession s = (MCRSession) (sessions.get(sessionID));

        if (s == null) {
            logger.warn("MCRSession with ID " + sessionID + " not cached any more");
        }

        return s;
    }

    /**
     * Constructs a unique session ID for this session, based on current time
     * and IP address of host where the code runs.
     */
    private static synchronized String buildSessionID() {
        String localip = getLocalIP();

        java.util.StringTokenizer st = new java.util.StringTokenizer(localip, ".");

        long sum = Integer.parseInt(st.nextToken());

        while (st.hasMoreTokens())
            sum = (sum << 8) + Integer.parseInt(st.nextToken());

        String address = Long.toString(sum, 36);
        address = "000000" + address;

        String prefix = address.substring(address.length() - 6);

        long now = System.currentTimeMillis();
        String suffix = Long.toString(now, 36);

        return prefix + "-" + suffix;
    }

    /**
     * Returns the unique ID of this session
     */
    public String getID() {
        return sessionID;
    }

    /**
     * @return Returns a list of all stored object keys within MCRSession as java.util.Ierator 
     */
    public Iterator getObjectsKeyList() {
    	return Collections.unmodifiableSet(map.keySet()).iterator();
    }
    
    /** returns the current user ID */
    public final String getCurrentUserID() {
        return userID.trim();
    }

    /** sets the current user ID */
    public final void setCurrentUserID(String userID) {
        this.userID = userID;
    }

    /** returns the current language */
    public final String getCurrentLanguage() {
        return language.trim();
    }

    /** sets the current language */
    public final void setCurrentLanguage(String language) {
        this.language = language;
    }

    /** returns the current document ID */
    public final String getCurrentDocumentID() {
        return CurrentDocumentID;
    }

    /** returns the current document ID */
    public final String getCurrentUserName() {
        return FullName;
    }

    /** sets the current user fullname */
    public final void setCurrentUserName(String userName) {
        this.FullName = userName;
    }

    /** sets the current document ID */
    public final void setCurrentDocumentID(String DocumentID) {
        this.CurrentDocumentID = DocumentID;
    }

    /** Write data to the logger for debugging purposes */
    public final void debug() {
        logger.debug("SessionID = " + sessionID);
        logger.debug("UserID    = " + userID);
        logger.debug("IP        = " + ip);
        logger.debug("language  = " + language);
    }

    /** Stores an object under the given key within the session * */
    public Object put(Object key, Object value) {
        return map.put(key, value);
    }

    /** Returns the object that was stored in the session under the given key * */
    public Object get(Object key) {
        return map.get(key);
    }
    
    public void deleteObject(Object key){
        map.remove(key);
    }

    /** Get the ip value to the local IP */
    public static final String getLocalIP() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (java.net.UnknownHostException ignored) {
            return "127.0.0.1";
        }
    }

    /** Get the current ip value */
    public String getCurrentIP() {
        return ip;
    }

    /** Set the ip to the given IP */
    public final void setCurrentIP(String newip) {
        java.util.StringTokenizer st = new java.util.StringTokenizer(newip, ".");
        if (st.countTokens() != 4) return;
        try {
        while (st.hasMoreTokens()) {
                int i = Integer.parseInt(st.nextToken());
                if ((i<0)||(i>255)){
                    return;
                }
        }
        } catch ( Exception e) {
            logger.debug("Exception while parsing new ip "+newip+" using old value.",e);
            return;
        }
        this.ip = newip;
    }
    
    public final long getLoginTime(){
        return loginTime;
    }

}
