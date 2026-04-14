/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.common.events;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;

/**
 * A EventHandler which runs as {@link MCRSystemUserInformation#getJanitorInstance()}.
 */
public class MCRJanitorEventHandlerBase extends MCREventHandlerBase {

    @Override
    public void doHandleEvent(MCREvent evt) {

        try (NonThrowingAutoClosable closer =
            MCRThreadBoundUserInformation.attach(MCRSystemUserInformation.getJanitorInstance())) {
            super.doHandleEvent(evt);
        }
    }

    private interface NonThrowingAutoClosable extends AutoCloseable {
        @Override
        void close();
    }

    private static class MCRThreadBoundUserInformation implements MCRUserInformation {

        private final Map<Thread, MCRUserInformation> threads = new ConcurrentHashMap<>();
        private final MCRUserInformation originalUserInformation;

        private MCRThreadBoundUserInformation(MCRUserInformation originalUserInformation) {
            this.originalUserInformation = originalUserInformation;
        }

        public static NonThrowingAutoClosable attach(MCRUserInformation newUserInformation) {
            MCRSession session = MCRSessionMgr.getCurrentSession();
            synchronized (session) {
                if (session.getUserInformation() instanceof MCRThreadBoundUserInformation threadBound) {
                    threadBound.threads.put(Thread.currentThread(), newUserInformation);
                    return () -> detach(session);
                } else {
                    MCRUserInformation oldUserInformation = session.getUserInformation();
                    session.setUserInformation(MCRSystemUserInformation.getGuestInstance());
                    MCRThreadBoundUserInformation threadBound =
                        new MCRThreadBoundUserInformation(oldUserInformation);
                    threadBound.threads.put(Thread.currentThread(), newUserInformation);
                    session.setUserInformation(threadBound);
                    return () -> detach(session);
                }
            }
        }

        public static void detach(MCRSession session) {
            synchronized (session) {
                if (session.getUserInformation() instanceof MCRThreadBoundUserInformation threadBound) {
                    threadBound.threads.remove(Thread.currentThread());
                    if (threadBound.threads.isEmpty()) {
                        session.setUserInformation(MCRSystemUserInformation.getGuestInstance());
                        session.setUserInformation(threadBound.originalUserInformation);
                    }
                }
            }
        }

        @Override
        public String getUserID() {
            MCRUserInformation ui = threads.get(Thread.currentThread());
            return ui != null ? ui.getUserID() : originalUserInformation.getUserID();
        }

        @Override
        public boolean isUserInRole(String role) {
            MCRUserInformation ui = threads.get(Thread.currentThread());
            return ui != null ? ui.isUserInRole(role) : originalUserInformation.isUserInRole(role);
        }

        @Override
        public String getUserAttribute(String attribute) {
            MCRUserInformation ui = threads.get(Thread.currentThread());
            return ui != null ? ui.getUserAttribute(attribute) : originalUserInformation.getUserAttribute(attribute);
        }

    }
}
