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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.frontend.servlets.MCRServlet;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * This Class will be stored in the a {@link javax.servlet.http.HttpSession} and can be used to resolve the
 * {@link MCRSession}. We can not store {@link MCRSession} directly in the {@link javax.servlet.http.HttpSession}
 * because values need to be {@link java.io.Serializable}.
 */
public final class MCRSessionResolver implements Serializable, HttpSessionBindingListener {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String sessionID;

    private MCRSessionResolver(final String sessionID) {
        this.sessionID = sessionID;
    }

    public MCRSessionResolver(final MCRSession session) {
        this(session.getID());
    }

    public final String getSessionID() {
        return sessionID;
    }

    /**
     * Tries to resolve the {@link MCRSession} throught the {@link MCRSessionMgr}
     *
     * @return if is already closed it will return a {@link Optional#empty()}
     */
    public final Optional<MCRSession> resolveSession() {
        return Optional.ofNullable(MCRSessionMgr.getSession(sessionID));
    }

    @Override
    public void valueBound(HttpSessionBindingEvent hsbe) {
        Object obj = hsbe.getValue();
        if (LOGGER.isDebugEnabled() && obj instanceof MCRSessionResolver) {
            LOGGER.debug("Bound MCRSession {} to HttpSession {}", ((MCRSessionResolver) obj).getSessionID(), hsbe.getSession().getId());
        }
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent hsbe) {
        // hsbe.getValue() does not work right with tomcat
        Optional<MCRSessionResolver> newSessionResolver = Optional
                .ofNullable(hsbe.getSession().getAttribute(MCRServlet.ATTR_MYCORE_SESSION))
                .filter(o -> o instanceof MCRSessionResolver)
                .map(MCRSessionResolver.class::cast);
        MCRSessionResolver oldResolver = this;
        if (newSessionResolver.isPresent() && !oldResolver.equals(newSessionResolver.get())) {
            LOGGER.warn("Attribute {} is beeing unbound from session {} and replaced by {}!", hsbe.getName(), oldResolver.getSessionID(), newSessionResolver.get());
            oldResolver.resolveSession().ifPresent(MCRSession::close);
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MCRSessionResolver that = (MCRSessionResolver) o;
        return Objects.equals(getSessionID(), that.getSessionID());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSessionID());
    }

    @Override
    public String toString() {
        return "Resolver to " + getSessionID();
    }
}
