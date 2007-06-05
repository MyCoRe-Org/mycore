/**
 * $RCSfile$
 * $Revision$ $Date$
 *
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
 *
 **/
package org.mycore.backend.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.context.CurrentSessionContext;
import org.hibernate.context.ThreadLocalSessionContext;
import org.hibernate.engine.SessionFactoryImplementor;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;

/**
 * A {@link CurrentSessionContext} implementation which scopes the notion of
 * current session by the current {@link MCRSession}. As the MCRSession can be
 * used by more than one {@link Thread} at a time and {@link Session} is not
 * threadsafe. This implementation allows the first thread of a
 * {@link MCRSession} to keep the {@link Session} open for a long conversation.
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
public class MCRSessionContext extends ThreadLocalSessionContext implements MCRSessionListener {

    private static final long serialVersionUID = -801352757721845792L;

    private static final String SESSION_KEY = "hibernateSession";

    private static final Logger LOGGER = Logger.getLogger(MCRSessionContext.class);

    ThreadLocal<Boolean> firstThread = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    public MCRSessionContext(SessionFactoryImplementor factory) {
        super(factory);
        MCRSessionMgr.addSessionListener(this);
    }

    public void sessionEvent(MCRSessionEvent event) {
        MCRSession mcrSession = event.getSession();
        Session currentSession;
        switch (event.getType()) {
        case activated:
            if (event.getConcurrentAccessors() < 1) {
                // mark this Thread as first Thread of MCRSession
                LOGGER.debug("First Thread to access " + mcrSession);
                firstThread.set(true);
            }
            break;
        case passivated:
            currentSession = unbind(factory);
            if (event.getConcurrentAccessors() < 2) {
                // save Session for later use;
                LOGGER.debug("Saving hibernate Session for later use in " + mcrSession);
                mcrSession.put(SESSION_KEY, currentSession);
            } else {
                autoCloseSession(currentSession);
            }
            // reset firstThread marker as this Session passivates now
            firstThread.remove();
            break;
        case destroyed:
            currentSession = unbind(factory);
            autoCloseSession(currentSession);
            Object obj = mcrSession.get(SESSION_KEY);
            if (obj != null && currentSession != obj) {
                autoCloseSession((Session) obj);
            }
            firstThread.remove();
            break;
        default:
            break;
        }
    }

    /**
     * Closes Session if Session is still open.
     */
    private void autoCloseSession(Session currentSession) {
        if (currentSession != null && currentSession.isOpen()) {
            LOGGER.debug("Autoclosing current hibernate Session");
            currentSession.close();
        }
    }

    @Override
    protected org.hibernate.classic.Session buildOrObtainSession() {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        if (firstThread.get()) {
            LOGGER.debug("First Thread to access " + mcrSession);
            LOGGER.debug("Try to reuse hibernate Session from current " + mcrSession);
            Object obj = mcrSession.get(SESSION_KEY);
            if (obj != null && ((Session) obj).isOpen()) {
                LOGGER.debug("Reusing old hibernate Session.");
                return (org.hibernate.classic.Session) obj;
            }
        }
        // creates a new one
        LOGGER.debug("Obtaining new hibernate Session.");
        org.hibernate.classic.Session session = super.buildOrObtainSession();
        if (mcrSession.get(SESSION_KEY) == null || firstThread.get()) {
            // must be a Sessions that started before this instance added as
            // MCRSessionListener or old Session was closed
            firstThread.set(true);
            mcrSession.put(SESSION_KEY, session);
        }
        return session;
    }

    @Override
    protected boolean isAutoCloseEnabled() {
        return false;
    }

    @Override
    protected boolean isAutoFlushEnabled() {
        return false;
    }

    @Override
    protected CleanupSynch buildCleanupSynch() {
        return new CleanupSynch(factory) {
            private static final long serialVersionUID = -7894370437708819993L;

            public void afterCompletion(int arg0) {
            }

            public void beforeCompletion() {
            }
        };
    }

}
