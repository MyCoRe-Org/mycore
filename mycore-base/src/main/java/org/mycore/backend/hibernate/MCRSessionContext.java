/**
 * 
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
import org.hibernate.context.internal.ThreadLocalSessionContext;
import org.hibernate.context.spi.CurrentSessionContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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
 * @version $Revision$ $Date: 2011-04-05 10:52:04 +0200 (Di, 05 Apr
 *          2011) $
 * @since 2.0
 */
public class MCRSessionContext extends ThreadLocalSessionContext implements MCRSessionListener {

    private static final long serialVersionUID = -801352757721845792L;

    private static final Logger LOGGER = Logger.getLogger(MCRSessionContext.class);

    public MCRSessionContext(SessionFactoryImplementor factory) {
        super(factory);
        MCRSessionMgr.addSessionListener(this);
    }

    public void sessionEvent(MCRSessionEvent event) {
        MCRSession mcrSession = event.getSession();
        Session currentSession;
        switch (event.getType()) {
            case activated:
                if (event.getConcurrentAccessors() <= 1) {
                    LOGGER.debug("First Thread to access " + mcrSession);
                }
                break;
            case passivated:
                currentSession = unbind(factory());
                autoCloseSession(currentSession);
                break;
            case destroyed:
                currentSession = unbind(factory());
                autoCloseSession(currentSession);
                break;
            case created:
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
    protected Session buildOrObtainSession() {
        // creates a new one
        LOGGER.debug("Obtaining new hibernate Session.");
        Session session = super.buildOrObtainSession();
        LOGGER.debug("Returning session with transaction: " + session.getTransaction());
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
    protected ThreadLocalSessionContext.CleanupSync buildCleanupSynch() {
        return new ThreadLocalSessionContext.CleanupSync(factory()) {
            private static final long serialVersionUID = -7894370437708819993L;

            @Override
            public void afterCompletion(int arg0) {
            }

            @Override
            public void beforeCompletion() {
            }
        };
    }

}
