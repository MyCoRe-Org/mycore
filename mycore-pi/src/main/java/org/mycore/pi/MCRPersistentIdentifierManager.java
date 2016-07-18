package org.mycore.pi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.backend.MCRPI_;

public class MCRPersistentIdentifierManager {

    public static final String PARSER_CONFIGURATION = "MCR.PI.Parsers.";
    public static final String RESOLVER_CONFIGURATION = "MCR.PI.Resolvers";
    private List<MCRPersistentIdentifierResolver<MCRPersistentIdentifier>> resolverList = new ArrayList<>();
    private List<Class<? extends MCRPersistentIdentifierParser<? extends MCRPersistentIdentifier>>> parserList = new ArrayList<>();

    private MCRPersistentIdentifierManager() {
        Map<String, String> parserPropertiesMap = MCRConfiguration.instance().getPropertiesMap(PARSER_CONFIGURATION);
        parserPropertiesMap.forEach((k, v) -> {
            String type = k.substring(PARSER_CONFIGURATION.length());
            try {
                @SuppressWarnings("unchecked")
                Class<? extends MCRPersistentIdentifierParser<?>> parserClass = (Class<? extends MCRPersistentIdentifierParser<?>>) Class
                    .forName(v);
                registerParser(type, parserClass);
            } catch (ClassNotFoundException e) {
                throw new MCRConfigurationException("Could not load class " + v + " defined in " + k);
            }
        });

        Stream.of(MCRConfiguration.instance().getString(RESOLVER_CONFIGURATION).split(","))
                .forEach(className -> {
                    try {
                        Class<MCRPersistentIdentifierResolver<MCRPersistentIdentifier>> resolverClass = (Class<MCRPersistentIdentifierResolver<MCRPersistentIdentifier>>) Class.forName(className);
                        Constructor<MCRPersistentIdentifierResolver<MCRPersistentIdentifier>> resolverClassConstructor = resolverClass.getConstructor();
                        MCRPersistentIdentifierResolver<MCRPersistentIdentifier> resolver = resolverClassConstructor.newInstance();
                        resolverList.add(resolver);
                    } catch (ClassNotFoundException e) {
                        throw new MCRConfigurationException(RESOLVER_CONFIGURATION + " contains " + className + " but the class could not be found!", e);
                    } catch (NoSuchMethodException e) {
                        throw new MCRConfigurationException("The class " + className + " has no default constructor!", e);
                    } catch (IllegalAccessException e) {
                        throw new MCRConfigurationException("Cannot invoke default constructor of " + className + "!", e);
                    } catch (InstantiationException e) {
                        throw new MCRConfigurationException("The class " + className + " seems to be abstract!", e);
                    } catch (InvocationTargetException e) {
                        throw new MCRConfigurationException("The default constructor of class " + className + " throws a exception!", e);
                    }
                });

    }

    private Map<String, Class<? extends MCRPersistentIdentifierParser>> typeParserMap = new ConcurrentHashMap<>();

    public static MCRPersistentIdentifierManager getInstance() {
        return ManagerInstanceHolder.instance;
    }

    private static MCRPersistentIdentifierParser<?> getParserInstance(
        Class<? extends MCRPersistentIdentifierParser> detectorClass) {
        try {
            return detectorClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    public int getCount() {
        return getCount(null);
    }

    public boolean exist(MCRPIRegistrationInfo mcrpiRegistrationInfo) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Number> rowCountQuery = cb.createQuery(Number.class);
        Root<MCRPI> pi = rowCountQuery.from(MCRPI.class);
        return em.createQuery(
                rowCountQuery
                        .select(cb.count(pi))
                        .where(cb.equal(pi.get(MCRPI_.type), mcrpiRegistrationInfo.getType()))
                        .where(cb.equal(pi.get(MCRPI_.additional), mcrpiRegistrationInfo.getAdditional()))
                        .where(cb.equal(pi.get(MCRPI_.identifier), mcrpiRegistrationInfo.getIdentifier()))
                        .where(cb.equal(pi.get(MCRPI_.service), mcrpiRegistrationInfo.getService()))
                        .where(cb.equal(pi.get(MCRPI_.mycoreID), mcrpiRegistrationInfo.getMycoreID()))
        ).getSingleResult().intValue() > 0;
    }

    public MCRPI get(String service,String mycoreID, String additional){
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Number> rowCountQuery = cb.createQuery(Number.class);
        CriteriaQuery<MCRPI> getQuery = cb.createQuery(MCRPI.class);
        Root<MCRPI> pi = getQuery.from(MCRPI.class);
        return em.createQuery(
                getQuery
                        .where(cb.equal(pi.get(MCRPI_.additional), additional))
                        .where(cb.equal(pi.get(MCRPI_.mycoreID), mycoreID))
                        .where(cb.equal(pi.get(MCRPI_.service), service))).getSingleResult();

    }

    public int getCount(String type) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Number> rowCountQuery = cb.createQuery(Number.class);
        Root<MCRPI> pi = rowCountQuery.from(MCRPI.class);
        return em.createQuery(
            rowCountQuery
                .select(cb.count(pi))
                .where(cb.equal(pi.get(MCRPI_.type), type)))
            .getSingleResult().intValue();
    }

    public void delete(String objectID, String additional, String type, String service) {
        Objects.requireNonNull(objectID, "objectId may not be null");
        Objects.requireNonNull(type, "type may not be null");
        Objects.requireNonNull(additional, "additional may not be null");
        Objects.requireNonNull(service, "service may not be null");
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRPI> getQuery = cb.createQuery(MCRPI.class);
        Root<MCRPI> pi = getQuery.from(MCRPI.class);
        em.remove(
            em.createQuery(
                getQuery
                    .where(
                        cb.equal(pi.get(MCRPI_.mycoreID), objectID),
                        cb.equal(pi.get(MCRPI_.type), type),
                        cb.equal(pi.get(MCRPI_.additional), additional),
                        cb.equal(pi.get(MCRPI_.service), service)))
                .getSingleResult());
    }

    public List<MCRPIRegistrationInfo> getList() {
        return getList(null, -1, -1);
    }

    public List<MCRPIRegistrationInfo> getList(int from, int count) {
        return getList(null, from, count);
    }

    public List<MCRPIRegistrationInfo> getList(String type, int from, int count) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRPIRegistrationInfo> getQuery = cb.createQuery(MCRPIRegistrationInfo.class);
        Root<MCRPI> pi = getQuery.from(MCRPI.class);
        CriteriaQuery<MCRPIRegistrationInfo> all = getQuery
                .select(pi);

        if (type != null) {
            all = all.where(cb.equal(pi.get(MCRPI_.type), type));
        }

        TypedQuery<MCRPIRegistrationInfo> typedQuery = em.createQuery(all);

        if (from != -1) {
            typedQuery = typedQuery.setFirstResult(from);
        }

        if (count != -1) {
            typedQuery = typedQuery.setMaxResults(count);
        }

        return typedQuery
            .getResultList();
    }

    public List<MCRPIRegistrationInfo> getRegistered(MCRObject object) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRPIRegistrationInfo> getQuery = cb.createQuery(MCRPIRegistrationInfo.class);
        Root<MCRPI> pi = getQuery.from(MCRPI.class);
        return em.createQuery(
            getQuery
                .select(pi)
                .where(
                    cb.equal(pi.get(MCRPI_.mycoreID), object.getId().toString())))
            .getResultList();
    }

    public List<MCRPIRegistrationInfo> getInfo(String identifier) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRPIRegistrationInfo> getQuery = cb.createQuery(MCRPIRegistrationInfo.class);
        Root<MCRPI> pi = getQuery.from(MCRPI.class);
        return em.createQuery(
                getQuery
                        .select(pi)
                        .where(
                                cb.equal(pi.get(MCRPI_.identifier), identifier)))
                .getResultList();
    }

    public MCRPersistentIdentifierParser getParserForType(String type) {
        return getParserInstance(typeParserMap.get(type));
    }

    public void registerParser(String type, Class<? extends MCRPersistentIdentifierParser<? extends MCRPersistentIdentifier>> parserClass) {
        this.parserList.add(parserClass);
        this.typeParserMap.put(type, parserClass);
    }


    public List<MCRPersistentIdentifierResolver<MCRPersistentIdentifier>> getResolvers() {
        return this.resolverList;
    }

    public Stream<? extends MCRPersistentIdentifier> get(String pi) {
        return parserList.stream()
                .map(MCRPersistentIdentifierManager::getParserInstance)
                .map(p -> p.parse(pi))
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    private static final class ManagerInstanceHolder {
        public static final MCRPersistentIdentifierManager instance = new MCRPersistentIdentifierManager();
    }

}
