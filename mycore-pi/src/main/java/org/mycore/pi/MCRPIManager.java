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

package org.mycore.pi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.backend.MCRPI_;

public class MCRPIManager {
    private static final String TYPE = "type";

    private static final String MCRID = "mcrId";

    private static final String SERVICE = "service";

    private static final String ADDITIONAL = "additional";

    private static final String PARSER_CONFIGURATION = "MCR.PI.Parsers.";

    private static final String RESOLVER_CONFIGURATION = "MCR.PI.Resolvers";

    private static MCRPIManager instance;

    private List<MCRPIResolver<MCRPersistentIdentifier>> resolverList;

    private List<Class<? extends MCRPIParser<? extends MCRPersistentIdentifier>>> parserList;

    private Map<String, Class<? extends MCRPIParser>> typeParserMap;

    private MCRPIManager() {
        resolverList = new ArrayList<>();
        parserList = new ArrayList<>();
        typeParserMap = new ConcurrentHashMap<>();

        Map<String, String> parserPropertiesMap = MCRConfiguration.instance().getPropertiesMap(PARSER_CONFIGURATION);
        parserPropertiesMap.forEach((k, v) -> {
            String type = k.substring(PARSER_CONFIGURATION.length());
            try {
                @SuppressWarnings("unchecked")
                Class<? extends MCRPIParser<?>> parserClass = (Class<? extends MCRPIParser<?>>) Class
                    .forName(v);
                registerParser(type, parserClass);
            } catch (ClassNotFoundException e) {
                throw new MCRConfigurationException("Could not load class " + v + " defined in " + k);
            }
        });

        Stream.of(MCRConfiguration.instance().getString(RESOLVER_CONFIGURATION).split(","))
            .forEach(className -> {
                try {
                    @SuppressWarnings("unchecked")
                    Class<MCRPIResolver<MCRPersistentIdentifier>> resolverClass = (Class<MCRPIResolver<MCRPersistentIdentifier>>) Class
                        .forName(className);
                    Constructor<MCRPIResolver<MCRPersistentIdentifier>> resolverClassConstructor = resolverClass
                        .getConstructor();
                    MCRPIResolver<MCRPersistentIdentifier> resolver = resolverClassConstructor
                        .newInstance();
                    resolverList.add(resolver);
                } catch (ClassNotFoundException e) {
                    throw new MCRConfigurationException(
                        RESOLVER_CONFIGURATION + " contains " + className + " but the class could not be found!",
                        e);
                } catch (NoSuchMethodException e) {
                    throw new MCRConfigurationException("The class " + className + " has no default constructor!", e);
                } catch (IllegalAccessException e) {
                    throw new MCRConfigurationException("Cannot invoke default constructor of " + className + "!", e);
                } catch (InstantiationException e) {
                    throw new MCRConfigurationException("The class " + className + " seems to be abstract!", e);
                } catch (InvocationTargetException e) {
                    throw new MCRConfigurationException(
                        "The default constructor of class " + className + " throws a exception!", e);
                }
            });

    }

    public synchronized static MCRPIManager getInstance() {
        if (instance == null) {
            instance = new MCRPIManager();
        }

        return instance;
    }

    @SuppressWarnings("unchecked")
    private static <T extends MCRPersistentIdentifier> MCRPIParser<T> getParserInstance(
        Class<? extends MCRPIParser> detectorClass) throws ClassCastException {
        try {
            return detectorClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
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
                .where(cb.equal(pi.get(MCRPI_.type), mcrpiRegistrationInfo.getType()),
                    cb.equal(pi.get(MCRPI_.additional), mcrpiRegistrationInfo.getAdditional()),
                    cb.equal(pi.get(MCRPI_.identifier), mcrpiRegistrationInfo.getIdentifier()),
                    cb.equal(pi.get(MCRPI_.service), mcrpiRegistrationInfo.getService()),
                    cb.equal(pi.get(MCRPI_.mycoreID), mcrpiRegistrationInfo.getMycoreID())))
            .getSingleResult()
            .intValue() > 0;
    }

    public MCRPI get(String service, String mycoreID, String additional) {
        return MCREntityManagerProvider
            .getCurrentEntityManager()
            .createNamedQuery("Get.PI.Additional", MCRPI.class)
            .setParameter(MCRID, mycoreID)
            .setParameter(ADDITIONAL, additional)
            .setParameter(SERVICE, service)
            .getSingleResult();
    }

    public List<MCRPIRegistrationInfo> getCreatedIdentifiers(MCRObjectID id, String type,
        String registrationServiceID) {
        return MCREntityManagerProvider
            .getCurrentEntityManager()
            .createNamedQuery("Get.PI.Created", MCRPIRegistrationInfo.class)
            .setParameter(MCRID, id.toString())
            .setParameter(TYPE, type)
            .setParameter(SERVICE, registrationServiceID)
            .getResultList();
    }

    public boolean isCreated(MCRObjectID id, String additional, String type, String registrationServiceID) {
        return MCREntityManagerProvider
            .getCurrentEntityManager()
            .createNamedQuery("Count.PI.Created", Number.class)
            .setParameter(MCRID, id.toString())
            .setParameter(TYPE, type)
            .setParameter(ADDITIONAL, additional)
            .setParameter(SERVICE, registrationServiceID)
            .getSingleResult()
            .shortValue() > 0;
    }

    public boolean isRegistered(MCRPI mcrPi) {
        return isRegistered(mcrPi.getMycoreID(), mcrPi.getAdditional(), mcrPi.getType(), mcrPi.getService());
    }

    public boolean isRegistered(MCRObjectID mcrId, String additional, String type, String registrationServiceID) {
        return isRegistered(mcrId.toString(), additional, type, registrationServiceID);
    }

    public boolean isRegistered(String mcrId, String additional, String type, String registrationServiceID) {
        return MCREntityManagerProvider
            .getCurrentEntityManager()
            .createNamedQuery("Count.PI.Registered", Number.class)
            .setParameter(MCRID, mcrId)
            .setParameter(TYPE, type)
            .setParameter(ADDITIONAL, additional)
            .setParameter(SERVICE, registrationServiceID)
            .getSingleResult()
            .shortValue() > 0;
    }

    public boolean hasRegistrationStarted(MCRObjectID mcrId, String additional, String type,
        String registrationServiceID) {
        return hasRegistrationStarted(mcrId.toString(), additional, type, registrationServiceID);
    }

    public boolean hasRegistrationStarted(String mcrId, String additional, String type, String registrationServiceID) {
        return MCREntityManagerProvider
            .getCurrentEntityManager()
            .createNamedQuery("Count.PI.RegistrationStarted", Number.class)
            .setParameter(MCRID, mcrId)
            .setParameter(TYPE, type)
            .setParameter(ADDITIONAL, additional)
            .setParameter(SERVICE, registrationServiceID)
            .getSingleResult()
            .shortValue() > 0;
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
        Objects.requireNonNull(service, "service may not be null");
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRPI> getQuery = cb.createQuery(MCRPI.class);
        Root<MCRPI> pi = getQuery.from(MCRPI.class);
        Predicate additionalPredicate = additional == null ?
            cb.isNull(pi.get(MCRPI_.additional)) : cb.equal(pi.get(MCRPI_.additional), additional);
        em.remove(
            em.createQuery(
                getQuery
                    .where(
                        cb.equal(pi.get(MCRPI_.mycoreID), objectID),
                        cb.equal(pi.get(MCRPI_.type), type),
                        additionalPredicate,
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
        CriteriaQuery<MCRPIRegistrationInfo> all = getQuery.select(pi);

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

        return typedQuery.getResultList();
    }

    public Integer setRegisteredDateForUnregisteredIdenifiers(
        String type,
        Function<MCRPIRegistrationInfo, Optional<Date>> dateProvider, Integer batchSize) {

        List<MCRPI> unregisteredIdentifiers = getUnregisteredIdentifiers(type, batchSize);
        unregisteredIdentifiers
            .forEach(ident -> dateProvider
                .apply(ident)
                .ifPresent(ident::setRegistered));

        return unregisteredIdentifiers.size();
    }

    public List<MCRPI> getUnregisteredIdentifiers(String type, int maxSize) {
        TypedQuery<MCRPI> getUnregisteredQuery = MCREntityManagerProvider
            .getCurrentEntityManager()
            .createNamedQuery("Get.PI.Unregistered", MCRPI.class)
            .setParameter("type", type);

        if (maxSize >= 0) {
            getUnregisteredQuery.setMaxResults(maxSize);
        }

        return getUnregisteredQuery.getResultList();
    }

    public List<MCRPI> getUnregisteredIdentifiers(String type) {
        return getUnregisteredIdentifiers(type, -1);
    }

    public List<MCRPIRegistrationInfo> getRegistered(MCRBase object) {
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

    public List<MCRPIRegistrationInfo> getInfo(MCRPersistentIdentifier identifier) {
        return getInfo(identifier.asString());
    }

    public List<MCRPIRegistrationInfo> getInfo(String identifier) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRPIRegistrationInfo> getQuery = cb.createQuery(MCRPIRegistrationInfo.class);
        Root<MCRPI> pi = getQuery.from(MCRPI.class);
        return em.createQuery(
            getQuery
                .select(pi)
                .where(cb.equal(pi.get(MCRPI_.identifier), identifier)))
            .getResultList();
    }

    public Optional<MCRPIRegistrationInfo> getInfo(String identifier, String type) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRPIRegistrationInfo> getQuery = cb.createQuery(MCRPIRegistrationInfo.class);
        Root<MCRPI> pi = getQuery.from(MCRPI.class);
        final List<MCRPIRegistrationInfo> resultList = em.createQuery(
            getQuery
                .select(pi)
                .where(cb.equal(pi.get(MCRPI_.identifier), identifier),cb.equal(pi.get(MCRPI_.type), type)))
            .getResultList();
        return resultList.size()==0 ? Optional.empty():Optional.of(resultList.get(0));
    }


    /**
     * Returns a parser for a specific type of persistent identifier.
     *
     * @param type the type which should be parsed
     * @param <T>  the type of {@link MCRPIParser} which should be returned.
     * @return a MCRPIParser
     * @throws ClassCastException when the wrong type is passed
     */
    @SuppressWarnings("WeakerAccess")
    public <T extends MCRPersistentIdentifier> MCRPIParser<T> getParserForType(String type)
        throws ClassCastException {
        return getParserInstance(typeParserMap.get(type));
    }

    /**
     * Registers a parser for a specific type of persistent identifier.
     *
     * @param type        the type of the parser
     * @param parserClass the class of the parser
     */
    @SuppressWarnings("WeakerAccess")
    public void registerParser(
        String type,
        Class<? extends MCRPIParser<? extends MCRPersistentIdentifier>> parserClass) {

        this.parserList.add(parserClass);
        this.typeParserMap.put(type, parserClass);
    }

    public List<MCRPIResolver<MCRPersistentIdentifier>> getResolvers() {
        return this.resolverList;
    }

    public Stream<MCRPersistentIdentifier> get(String pi) {
        return parserList
            .stream()
            .map(MCRPIManager::getParserInstance)
            .map(p -> p.parse(pi))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(MCRPersistentIdentifier.class::cast);
    }
}
