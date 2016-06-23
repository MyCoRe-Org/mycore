package org.mycore.backend.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

public class MCREntityManagerProvider {

    private static EntityManagerFactory factory;

    private static MCRSessionContext context;

    private static PersistenceException initException;

    public static EntityManagerFactory getEntityManagerFactory() {
        return factory;
    }

    public static EntityManager getCurrentEntityManager() {
        if (context == null && initException != null) {
            throw initException;
        }
        return context.getCurrentEntityManager();
    }

    static void init(EntityManagerFactory factory) {
        MCREntityManagerProvider.factory = factory;
        context = new MCRSessionContext(factory);
    }

    static void init(PersistenceException e) {
        initException = e;
    }

}
