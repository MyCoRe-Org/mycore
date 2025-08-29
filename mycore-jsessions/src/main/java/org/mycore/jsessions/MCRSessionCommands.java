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

package org.mycore.jsessions;

import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionResolver;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.events.MCRServletContextHolder;
import org.mycore.common.log.MCRTableMessage;
import org.mycore.common.log.MCRTableMessage.Column;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.jsessions.MCRSessionStore.Sessions;

import jakarta.servlet.http.HttpSession;

@MCRCommandGroup(name = "HTTP Session Commands")
public class MCRSessionCommands extends MCRAbstractCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "list HTTP sessions",
        help = "list HTTP sessions",
        order = 10)
    public static synchronized void listHttpSessions() {

        MCRTableMessage<HttpSession> table = new MCRTableMessage<>(
            new Column<>("HTTP ID", HttpSession::getId),
            new Column<>("MCR ID", httpSession -> getSessionId(httpSession).orElse("")),
            new Column<>("Session", httpSession -> getSession(httpSession).map(session -> "yes").orElse("")),
            new Column<>("Creation Time", httpSession -> new Date(httpSession.getCreationTime())),
            new Column<>("Last Access Time", httpSession -> new Date(httpSession.getLastAccessedTime())));

        forEachHttpSession(table::add);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(table.logMessage("sessions"));
        }

    }

    @MCRCommand(syntax = "list HTTP and corresponding MyCoRe sessions",
        help = "list HTTP and corresponding MyCoRe sessions",
        order = 20)
    public static synchronized void listSessions() {

        MCRTableMessage<Sessions> table = new MCRTableMessage<>(
            new Column<>("HTTP ID", sessions -> sessions.httpSession().getId()),
            new Column<>("MCR ID", sessions -> sessions.session().getID()),
            new Column<>("Current IP", sessions -> sessions.session().getCurrentIP()),
            new Column<>("User", sessions -> sessions.session().getUserInformation()),
            new Column<>("Creation Time", sessions -> new Date(sessions.session().getCreateTime())),
            new Column<>("Last Access Time", sessions -> new Date(sessions.session().getLastAccessedTime())),
            new Column<>("First URI", sessions -> sessions.session().getFirstURI().map(URI::toString).orElse("")),
            new Column<>("Last URI", sessions -> sessions.session().getLastURI().map(URI::toString).orElse("")));

        forEachSession(table::add);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(table.logMessage("sessions"));
        }
        
    }

    @MCRCommand(syntax = "invalidate inactive HTTP guest sessions idle for more than {0} minutes",
        help = "invalidate HTTP guest sessions are idle for more than {0} minutes.",
        order = 30)
    public static synchronized void invalidateInactiveSessions(String olderThanMinutes) {

        long cutoffTime = System.currentTimeMillis() - Integer.parseInt(olderThanMinutes) * 60L * 1000L;

        Set<HttpSession> inactiveSessions = new HashSet<>();
        forEachSession(sessions -> {
            MCRSession session = sessions.session();
            if (session.getLastAccessedTime() < cutoffTime) {
                if (isGuestSession(session)) {
                    inactiveSessions.add(sessions.httpSession());
                }
            }
        });

        LOGGER.info("invalidating {} inactive sessions", inactiveSessions::size);
        LOGGER.trace("invalidating inactive sessions: {}", () -> joinSessionIds(inactiveSessions));

        inactiveSessions.forEach(HttpSession::invalidate);

    }

    @MCRCommand(syntax = "invalidate single hit HTTP guest sessions idle for more than {0} minutes",
        help = "invalidate HTTP guest sessions that performed a single request and are idle for more than {0} minutes.",
        order = 40)
    public static synchronized void invalidateSingleHitSessions(String olderThanMinutes) {

        long cutoffTime = System.currentTimeMillis() - Integer.parseInt(olderThanMinutes) * 60L * 1000L;

        Set<HttpSession> singleHitSessions = new HashSet<>();
        forEachSession(sessions -> {
            MCRSession session = sessions.session();
            if (session.getLastAccessedTime() < cutoffTime) {
                if (isGuestSession(session)) {
                    session.getFirstURI().ifPresent(firstUri -> {
                        session.getLastURI().ifPresent(lastUri -> {
                            if (firstUri.equals(lastUri)) {
                                singleHitSessions.add(sessions.httpSession());
                            }
                        });
                    });
                }
            }
        });

        LOGGER.info("invalidating {} single hit sessions", singleHitSessions::size);
        LOGGER.trace("invalidating single hit sessions: {}", () -> joinSessionIds(singleHitSessions));

        singleHitSessions.forEach(HttpSession::invalidate);

    }

    private static boolean isGuestSession(MCRSession session) {
        return session.getUserInformation() instanceof MCRSystemUserInformation systemUserInformation
            && systemUserInformation == MCRSystemUserInformation.GUEST;
    }

    private static void forEachHttpSession(Consumer<HttpSession> consumer) {
        MCRServletContextHolder.getInstance().get()
            .flatMap(MCRSessionStoreListener::getSessionStore)
            .ifPresent(sessionStore -> sessionStore.httpSessions().forEach(consumer));
    }

    private static void forEachSession(Consumer<Sessions> consumer) {
        MCRServletContextHolder.getInstance().get()
            .flatMap(MCRSessionStoreListener::getSessionStore)
            .ifPresent(sessionStore -> sessionStore.sessions().forEach(consumer));
    }

    private static Optional<String> getSessionId(HttpSession httpSession) {
        return Optional.ofNullable(httpSession.getAttribute(MCRServlet.ATTR_MYCORE_SESSION))
            .map(MCRSessionResolver.class::cast).map(MCRSessionResolver::getSessionID);
    }

    private static Optional<MCRSession> getSession(HttpSession httpSession) {
        return Optional.ofNullable(httpSession.getAttribute(MCRServlet.ATTR_MYCORE_SESSION))
            .map(MCRSessionResolver.class::cast).flatMap(MCRSessionResolver::resolveSession);
    }

    private static String joinSessionIds(Set<HttpSession> singleHitSessions) {
        return singleHitSessions.stream().map(HttpSession::getId).collect(Collectors.joining());
    }

}
