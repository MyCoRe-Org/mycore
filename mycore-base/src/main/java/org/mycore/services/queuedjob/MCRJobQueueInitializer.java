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
                MCRJobQueue.getInstance((Class)clazz).notifyListener();
            }
        }
    }
}
