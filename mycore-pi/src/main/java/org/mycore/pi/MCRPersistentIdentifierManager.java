package org.mycore.pi;

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

    private MCRPersistentIdentifierManager() {
        Map<String, String> propertiesMap = MCRConfiguration.instance().getPropertiesMap(PARSER_CONFIGURATION);
        propertiesMap.forEach((k, v) -> {
            String type = k.substring(PARSER_CONFIGURATION.length());
            try {
                @SuppressWarnings("unchecked")
                Class<? extends MCRPersistentIdentifierParser> parserClass = (Class<? extends MCRPersistentIdentifierParser>) Class
                    .forName(v);
                registerParser(type, parserClass);
            } catch (ClassNotFoundException e) {
                throw new MCRConfigurationException("Could not load class " + v + " defined in " + k);
            }
        });
    }

    private List<Class<? extends MCRPersistentIdentifierParser>> parserList = new ArrayList<>();

    private Map<String, Class<? extends MCRPersistentIdentifierParser>> typeParserMap = new ConcurrentHashMap<>();

    public static MCRPersistentIdentifierManager getInstance() {
        return ManagerInstanceHolder.instance;
    }

    private static MCRPersistentIdentifierParser getParserInstance(
        Class<? extends MCRPersistentIdentifierParser> detectorClass) {
        try {
            return detectorClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    public static int getCount() {
        return getCount(null);
    }

    public static boolean exist(MCRPIRegistrationInfo mcrpiRegistrationInfo) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Number> rowCountQuery = cb.createQuery(Number.class);
        Root<MCRPI> pi = rowCountQuery.from(MCRPI.class);
        return em.createQuery(
                rowCountQuery
                        .select(cb.count(pi))
                        .where(cb.equal(pi.get(MCRPI_.type), mcrpiRegistrationInfo.getType()))
                        .where(cb.equal(pi.get(MCRPI_.additional), mcrpiRegistrationInfo.getAdditional()))
                        .where(cb.equal(pi.get(MCRPI_.created), mcrpiRegistrationInfo.getCreated()))
                        .where(cb.equal(pi.get(MCRPI_.identifier), mcrpiRegistrationInfo.getIdentifier()))
                        .where(cb.equal(pi.get(MCRPI_.mcrRevision), mcrpiRegistrationInfo.getMcrRevision()))
                        .where(cb.equal(pi.get(MCRPI_.service), mcrpiRegistrationInfo.getService()))
                        .where(cb.equal(pi.get(MCRPI_.mcrVersion), mcrpiRegistrationInfo.getMcrVersion()))
                        .where(cb.equal(pi.get(MCRPI_.registered), mcrpiRegistrationInfo.getRegistered()))
                        .where(cb.equal(pi.get(MCRPI_.mycoreID), mcrpiRegistrationInfo.getMycoreID()))
        ).getSingleResult().intValue() > 0;

    }

    public static int getCount(String type) {
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

    public static void delete(String objectID, String additional, String type, String service) {
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

    public static List<MCRPIRegistrationInfo> getList() {
        return getList(null, -1, -1);
    }

    public static List<MCRPIRegistrationInfo> getList(int from, int count) {
        return getList(null, from, count);
    }

    public static List<MCRPIRegistrationInfo> getList(String type, int from, int count) {
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

    public static List<MCRPIRegistrationInfo> getRegistered(MCRObject object) {
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

    public void registerParser(String type, Class<? extends MCRPersistentIdentifierParser> parserClass) {
        this.parserList.add(parserClass);
        this.typeParserMap.put(type, parserClass);
    }

    public MCRPersistentIdentifierParser getParserForType(String type) {
        return getParserInstance(typeParserMap.get(type));
    }

    public Stream<MCRPersistentIdentifier> get(String pi) {
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
