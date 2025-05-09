/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.backend.jpa;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * A helper class which scopes the notion of
 * current session by the current {@link MCRSession}. This implementation allows a thread of a
 * {@link MCRSession} to keep the {@link EntityManager} open for a long conversation.
 *
 * @author Thomas Scheffler (yagee)
 *
 * @since 2016.03
 */
class MCRSessionContext implements MCRSessionListener {

    private static final Logger LOGGER = LogManager.getLogger();

    private EntityManagerFactory factory;

    private ThreadLocal<EntityManager> context;

    MCRSessionContext(EntityManagerFactory factory) {
        this.factory = factory;
        this.context = new ThreadLocal<>();
        MCRSessionMgr.addSessionListener(this);
    }

    @Override
    public void sessionEvent(MCRSessionEvent event) {
        MCRSession mcrSession = event.getSession();
        EntityManager currentEntityManager;
        switch (event.getType()) {
            case ACTIVATED:
                if (event.getConcurrentAccessors() <= 1) {
                    LOGGER.debug(() -> "First Thread to access " + mcrSession);
                }
                break;
            case PASSIVATED:
                currentEntityManager = unbind();
                autoCloseSession(currentEntityManager);
                break;
            case DESTROYED:
                currentEntityManager = unbind();
                autoCloseSession(currentEntityManager);
                break;
            case CREATED:
                break;
            default:
                break;
        }
    }

    private EntityManager unbind() {
        EntityManager entityManager = context.get();
        context.set(null);
        return entityManager;
    }

    EntityManager getCurrentEntityManager() {
        return Optional
            .ofNullable(context.get())
            .filter(EntityManager::isOpen)
            .orElseGet(this::createEntityManagerInContext);
    }

    /**
     * Closes Session if Session is still open.
     */
    private void autoCloseSession(EntityManager currentEntityManager) {
        if (currentEntityManager != null && currentEntityManager.isOpen()) {
            LOGGER.debug("Autoclosing current JPA EntityManager");
            currentEntityManager.close();
        }
    }

    private EntityManager createEntityManagerInContext() {
        // creates a new one
        LOGGER.debug("Obtaining new entity manager.");
        EntityManager entityManager = factory.createEntityManager();
        LOGGER.debug(() -> "Returning entity manager with "
            + (entityManager.getTransaction().isActive() ? "active" : "non-active")
            + " transaction.");
        context.set(entityManager);
        return entityManager;
    }

}
