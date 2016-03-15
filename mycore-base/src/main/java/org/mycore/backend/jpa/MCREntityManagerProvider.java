package org.mycore.backend.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

public class MCREntityManagerProvider {

    private static EntityManagerFactory factory;

    private static MCRSessionContext context;

    public static EntityManagerFactory getEntityManagerFactory() {
        return factory;
    }

    public static EntityManager getCurrentEntityManager() {
        return context.getCurrentEntityManager();
    }

    static void init(EntityManagerFactory factory) {
        MCREntityManagerProvider.factory = factory;
        context = new MCRSessionContext(factory);
    }

}
