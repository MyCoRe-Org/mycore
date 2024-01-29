package org.mycore.backend.jpa;

import jakarta.persistence.Cache;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRPostConstruction;

import java.util.HashMap;
import java.util.Map;

public class MCREntityManagerFactory implements EntityManagerFactory {

    private EntityManagerFactoryBuilderImpl builder;

    private EntityManagerFactory entityManagerFactory;

    private PersistenceUnitDescriptor persistenceUnitDescriptor;

    private static final Logger LOGGER = LogManager.getLogger();

    public MCREntityManagerFactory() {
        LOGGER.info("Creating MCREntityManagerFactory");
    }

    @MCRPostConstruction
    public void init() {
        builder = new EntityManagerFactoryBuilderImpl(persistenceUnitDescriptor, new HashMap<>());
        entityManagerFactory = builder.build();
    }

    @MCRInstance(name = "PersistenceUnitDescriptor", valueClass = PersistenceUnitDescriptor.class, required = true)
    public void setPersistenceUnitDescriptor(PersistenceUnitDescriptor persistenceUnitDescriptor) {
        this.persistenceUnitDescriptor = persistenceUnitDescriptor;
    }

    @Override
    public EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    @Override
    public EntityManager createEntityManager(Map map) {
        return entityManagerFactory.createEntityManager(map);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        return entityManagerFactory.createEntityManager(synchronizationType);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
        return entityManagerFactory.createEntityManager(synchronizationType, map);
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return entityManagerFactory.getCriteriaBuilder();
    }

    @Override
    public Metamodel getMetamodel() {
        return entityManagerFactory.getMetamodel();
    }

    @Override
    public boolean isOpen() {
        return entityManagerFactory.isOpen();
    }

    @Override
    public void close() {
        entityManagerFactory.close();
    }

    @Override
    public Map<String, Object> getProperties() {
        return entityManagerFactory.getProperties();
    }

    @Override
    public Cache getCache() {
        return entityManagerFactory.getCache();
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        return entityManagerFactory.getPersistenceUnitUtil();
    }

    @Override
    public void addNamedQuery(String name, Query query) {
        entityManagerFactory.addNamedQuery(name, query);
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        return entityManagerFactory.unwrap(cls);
    }

    @Override
    public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
        entityManagerFactory.addNamedEntityGraph(graphName, entityGraph);
    }

    public EntityManagerFactoryBuilderImpl getBuilder() {
        return builder;
    }

    public PersistenceUnitDescriptor getPersistenceUnitDescriptor() {
        return persistenceUnitDescriptor;
    }
}
