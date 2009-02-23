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

package org.mycore.common;

import static org.mycore.common.events.MCRSessionEvent.Type.activated;
import static org.mycore.common.events.MCRSessionEvent.Type.passivated;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;
import org.mycore.datamodel.classifications.MCRClassificationBrowserData;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * Instances of this class collect information kept during a session like the
 * currently active user, the preferred language etc.
 * 
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @author Frank L\u00fctzenkirchen
 * 
 * @version $Revision$ $Date: 2008-03-17 17:12:15 +0100 (Mo, 17 Mrz
 *          2008) $
 */
public class MCRSession implements Cloneable {
    /** A map storing arbitrary session data * */
    private Map map = new HashMap();

    AtomicInteger accessCount;

    AtomicInteger concurrentAccess;

    ThreadLocal<AtomicInteger> currentThreadCount = new ThreadLocal<AtomicInteger>() {
        public AtomicInteger initialValue() {
            return new AtomicInteger();
        }
    };

    /** the logger */
    static Logger LOGGER = Logger.getLogger(MCRSession.class.getName());

    /** The user ID of the session */
    private String userID = null;

    /** The language for this session as upper case character */
    private String language = null;

    /** The unique ID of this session */
    private String sessionID = null;

    /** -ASC- for MCRClassificationBrowser Class session data */
    public MCRClassificationBrowserData BData = null;

    private String FullName = null;

    private String CurrentDocumentID = null;

    private String ip = null;

    private long loginTime, lastAccessTime, thisAccessTime, createTime;

    private boolean dataBaseAccess;

    private Transaction transaction = null;

    /**
     * The constructor of a MCRSession. As default the user ID is set to the
     * value of the property variable named 'MCR.Users.Guestuser.UserName'.
     */
    MCRSession() {
        MCRConfiguration config = MCRConfiguration.instance();
        userID = config.getString("MCR.Users.Guestuser.UserName", "gast");
        language = config.getString("MCR.Metadata.DefaultLang", "de");
        dataBaseAccess = MCRConfiguration.instance().getBoolean("MCR.Persistence.Database.Enable", true);

        accessCount = new AtomicInteger();
        concurrentAccess = new AtomicInteger();

        ip = "";
        sessionID = buildSessionID();
        MCRSessionMgr.addSession(this);

        LOGGER.debug("MCRSession created " + sessionID);
        setLoginTime();
        createTime = loginTime;

    }

    public final void setLoginTime() {
        loginTime = System.currentTimeMillis();
        lastAccessTime = loginTime;
        thisAccessTime = loginTime;
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
     * @return Returns a list of all stored object keys within MCRSession as
     *         java.util.Ierator
     */
    public Iterator getObjectsKeyList() {
        return Collections.unmodifiableSet(map.keySet()).iterator();
    }

    /** returns the current user ID */
    public final String getCurrentUserID() {
        return userID;
    }

    /** sets the current user ID */
    public final void setCurrentUserID(String userID) {
        this.userID = userID;
    }

    /** returns the current language */
    public final String getCurrentLanguage() {
        return language;
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
        LOGGER.debug("SessionID = " + sessionID);
        LOGGER.debug("UserID    = " + userID);
        LOGGER.debug("IP        = " + ip);
        LOGGER.debug("language  = " + language);
    }

    /** Stores an object under the given key within the session * */
    public Object put(Object key, Object value) {
        return map.put(key, value);
    }

    /** Returns the object that was stored in the session under the given key * */
    public Object get(Object key) {
        return map.get(key);
    }

    public void deleteObject(Object key) {
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
        if (st.countTokens() != 4)
            return;
        try {
            while (st.hasMoreTokens()) {
                int i = Integer.parseInt(st.nextToken());
                if ((i < 0) || (i > 255)) {
                    return;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Exception while parsing new ip " + newip + " using old value.", e);
            return;
        }
        this.ip = newip;
    }

    public final long getLoginTime() {
        return loginTime;
    }

    public void close() {
        // remove from session list
        LOGGER.debug("Remove myself from MCRSession list");
        MCRSessionMgr.removeSession(this);
        // clear bound objects
        LOGGER.debug("Clearing local map.");
        map.clear();
        this.sessionID = null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MCRSession[");
        sb.append(getID());
        sb.append(",user:'");
        sb.append(getCurrentUserID());
        sb.append("',ip:");
        sb.append(getCurrentIP());
        sb.append("]");
        return sb.toString();
    }

    public long getLastAccessedTime() {
        return lastAccessTime;
    }

    /**
     * Activate this session. For internal use mainly by MCRSessionMgr.
     * 
     * @see MCRSessionMgr#setCurrentSession(MCRSession)
     */
    void activate() {
        lastAccessTime = thisAccessTime;
        thisAccessTime = System.currentTimeMillis();
        accessCount.incrementAndGet();
        if (currentThreadCount.get().getAndIncrement() == 0) {
            fireSessionEvent(activated, concurrentAccess.incrementAndGet());
        } else {
            try {
                throw new MCRException("Cannot activate a Session more than once per thread: " + currentThreadCount.get().get());
            } catch (Exception e) {
                LOGGER.debug("Too many activate() calls stacktrace:", e);
            }
        }
    }

    /**
     * Passivate this session. For internal use mainly by MCRSessionMgr.
     * 
     * @see MCRSessionMgr#releaseCurrentSession()
     */
    void passivate() {
        if (currentThreadCount.get().getAndDecrement() == 1) {
            fireSessionEvent(passivated, concurrentAccess.decrementAndGet());
        } else {
            LOGGER.debug("deactivate currentThreadCount: " + currentThreadCount.get().get());
        }
    }

    /**
     * Fire MCRSessionEvents.
     * 
     * This is a common method that fires all types of MCRSessionEvent.
     * 
     * Mainly for internal use of MCRSession and MCRSessionMgr.
     * 
     * @param type
     *            type of event
     * @param concurrentAccessors
     *            number of concurrentThreads (passivateEvent gets 0 for
     *            singleThread)
     */
    void fireSessionEvent(MCRSessionEvent.Type type, int concurrentAccessors) {
        List<MCRSessionListener> listeners = MCRSessionMgr.getListeners();
        if (listeners.size() == 0) {
            return;
        }
        MCRSessionEvent event = new MCRSessionEvent(this, type, concurrentAccessors);
        LOGGER.debug(event);
        MCRSessionMgr.getListenersLock().readLock().lock();
        MCRSessionListener[] list = listeners.toArray(new MCRSessionListener[listeners.size()]);
        MCRSessionMgr.getListenersLock().readLock().unlock();
        for (MCRSessionListener listener : list) {
            listener.sessionEvent(event);
        }
    }

    public long getThisAccessTime() {
        return thisAccessTime;
    }

    public long getCreateTime() {
        return createTime;
    }
    
    public Principal getUserPrincipal(){
        MCRServletJob job=(MCRServletJob) get("MCRServletJob");
        if (job==null)
            return null;
        return job.getRequest().getUserPrincipal();
    }
    
    public boolean isPrincipalInRole(String role){
        Principal p=getUserPrincipal();
        if (p==null)
            return false;
        MCRServletJob job=(MCRServletJob) get("MCRServletJob");
        if (job==null)
            return false;
        return job.getRequest().isUserInRole(role);
    }

    /**
     * starts a new database transaction.
     */
    public void beginTransaction() {
        if (dataBaseAccess)
            transaction = MCRHIBConnection.instance().getSession().beginTransaction();
    }

    /**
     * commits the database transaction.
     * Commit is only done if {@link #isTransactionActive()} returns true.
     */
    public void commitTransaction() {
        if (isTransactionActive()) {
            transaction.commit();
            beginTransaction();
            MCRHIBConnection.instance().getSession().clear();
            transaction.commit();
        }
    }

    /**
     * forces the database transaction to roll back.
     * Roll back is only performed if {@link #isTransactionActive()} returns true.
     */
    public void rollbackTransaction() {
        if (isTransactionActive()) {
            transaction.rollback();
            MCRHIBConnection.instance().getSession().close();
        }
    }

    /**
     * Is the transaction still alive?
     * @return true if the transaction is still alive
     */
    public boolean isTransactionActive() {
        return dataBaseAccess && transaction != null && transaction.isActive();
    }

}
