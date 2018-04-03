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

package org.mycore.common;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * This Class will be stored in the a {@link javax.servlet.http.HttpSession} and can be used to resolve the
 * {@link MCRSession}. We can not store {@link MCRSession} directly in the {@link javax.servlet.http.HttpSession}
 * because values need to be {@link java.io.Serializable}.
 */
public final class MCRSessionResolver implements Serializable, HttpSessionBindingListener {

    private static final ConcurrentHashMap<String, MCRSessionResolver> INSTANCE_MAP = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LogManager.getLogger();

    private final String sessionID;

    private MCRSessionResolver(final String sessionID) {
        this.sessionID = sessionID;
    }

    /**
     * Creates or Gets the resolver of a {@link MCRSession}.
     * @param session which will be resolved
     * @return a resolver for the {@link MCRSession}
     */
    public static MCRSessionResolver instanceOf(final MCRSession session) {
        return INSTANCE_MAP.computeIfAbsent(session.getID(), MCRSessionResolver::new);
    }

    public final String getSessionID() {
        return sessionID;
    }

    /**
     * Tries to resolve the {@link MCRSession} throught the {@link MCRSessionMgr}
     * @return if is already closed it will return a {@link Optional#empty()}
     */
    public final Optional<MCRSession> resolveSession() {
        Optional<MCRSession> session = Optional.ofNullable(MCRSessionMgr.getSession(sessionID));

        if (!session.isPresent()) {
            INSTANCE_MAP.remove(sessionID);
        }

        return session;
    }

    @Override
    public void valueBound(HttpSessionBindingEvent hsbe) {
        Object obj = hsbe.getValue();
        if (LOGGER.isDebugEnabled() && obj instanceof MCRSession) {
            LOGGER.debug("Bound MCRSession {} to HttpSession {}", obj, hsbe.getSession().getId());
        }
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent hsbe) {
        Object obj = hsbe.getValue();
        if (obj instanceof MCRSession) {
            Optional<MCRSessionResolver> newSessionResolver = Optional
                .ofNullable(hsbe.getSession().getAttribute(MCRServlet.ATTR_MYCORE_SESSION))
                .map(MCRSessionResolver.class::cast);

            MCRSessionResolver resolver = null;
            if (!newSessionResolver.isPresent() || !(resolver = newSessionResolver.get()).equals(this)) {
                LOGGER.warn("Attribute {} is beeing unbound from session!", hsbe.getName());
                if (resolver != null) {
                    LOGGER.warn("And replaced by {}", resolver);
                }
                MCRSession mcrSession = (MCRSession) obj;
                mcrSession.close();
            }

        }
    }

    @Override
    public String toString() {
        return "Resolver to " + getSessionID();
    }
}
