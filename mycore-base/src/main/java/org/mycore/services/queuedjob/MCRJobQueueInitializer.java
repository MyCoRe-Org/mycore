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

package org.mycore.services.queuedjob;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.events.MCRStartupHandler;

public class MCRJobQueueInitializer implements MCRStartupHandler.AutoExecutable {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        if (MCRHIBConnection.isEnabled()) {
            EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
            TypedQuery<Object> query = em.createNamedQuery("mcrjob.classes", Object.class);
            List<Object> resultList = query.getResultList();
            for (Object clazz : resultList) {
                LOGGER.info("Initialize MCRJobQueue for " + clazz);
                MCRJobQueue.getInstance((Class) clazz).notifyListener();
            }
        }
    }
}
