package org.mycore.pi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.pi.backend.MCRPI;

public class MCRPersistentIdentifierManager {

    public static final String PARSER_CONFIGURATION = "MCR.PI.Parsers.";

    private MCRPersistentIdentifierManager(){
        Map<String, String> propertiesMap = MCRConfiguration.instance().getPropertiesMap(PARSER_CONFIGURATION);
        propertiesMap.forEach((k,v)->{
            String type = k.substring(PARSER_CONFIGURATION.length());
            try {
                Class<?> parserClass = Class.forName(v);
                registerParser(type, (Class<? extends MCRPersistentIdentifierParser>) parserClass);
            } catch (ClassNotFoundException e) {
                throw  new MCRConfigurationException("Could not load class " + v + " defined in " + k);
            }
        });
    }

    private List<Class<? extends MCRPersistentIdentifierParser>> parserList = new ArrayList<>();
    private Map<String, Class<? extends MCRPersistentIdentifierParser>> typeParserMap = new ConcurrentHashMap<>();

    public static MCRPersistentIdentifierManager getInstance() {
        return ManagerInstanceHolder.instance;
    }

    private static MCRPersistentIdentifierParser getParserInstance(Class<? extends MCRPersistentIdentifierParser> detectorClass) {
        try {
            return detectorClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    public static int getCount() {
        return getCount(null);
    }

    public static int getCount(String type) {
        return getTypeCriteria(type)
                .setProjection(Projections.rowCount())
                .uniqueResult()
                .hashCode();
    }

    public static List<MCRPIRegistrationInfo> getList(int from, int count) {
        return getList(null, from, count);
    }

    public static List<MCRPIRegistrationInfo> getList(String type, int from, int count) {
        Criteria criteria = getTypeCriteria(type);
        return criteria
                .setMaxResults(count)
                .setFirstResult(from)
                .list();
    }

    public static List<MCRPIRegistrationInfo> getRegistered(MCRObject object){
        return getObjectCriteria(object.getId().toString()).list();
    }

    private static Criteria getTypeCriteria(String type) {
        Criteria criteria = getCriteria();

        if (type != null) {
            criteria.add(Restrictions.eq("type", type));
        }
        return criteria;
    }

    private static Criteria getObjectCriteria(String objectID){
        Criteria criteria = getCriteria();

        if (objectID != null) {
            criteria.add(Restrictions.eq("mycoreID", objectID));
        }
        return criteria;
    }

    private static Criteria getCriteria() {
        return MCRHIBConnection.instance()
                    .getSession()
                    .createCriteria(MCRPI.class);
    }

    public void registerParser(String type, Class<? extends MCRPersistentIdentifierParser> parserClass) {
        this.parserList.add(parserClass);
        this.typeParserMap.put(type, parserClass);
    }

    public MCRPersistentIdentifierParser getParserForType(String type){
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
