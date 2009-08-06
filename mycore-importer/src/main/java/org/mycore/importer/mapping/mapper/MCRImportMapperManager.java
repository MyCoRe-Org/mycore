package org.mycore.importer.mapping.mapper;

import java.util.Hashtable;

/**
 * This class provides access to all MCRImportMapper classes.
 * These class objects are stored in a hashtable. The method createMapperInstance
 * allows to create instances of them.
 * 
 * @author Matthias Eichner
 */
public class MCRImportMapperManager {

    private Hashtable<String, Class<? extends MCRImportMapper>> mapperTable;

    public MCRImportMapperManager() {
        addDefaultMappers();
    }

    private void addDefaultMappers() {
        // the default mappers
        mapperTable = new Hashtable<String, Class<? extends MCRImportMapper>>();
        mapperTable.put("metadata", MCRImportMetadataMapper.class);
        mapperTable.put("id", MCRImportIdMapper.class);
        mapperTable.put("label", MCRImportLabelMapper.class);
        mapperTable.put("parent", MCRImportParentMapper.class);
        mapperTable.put("classification", MCRImportClassificationMapper.class);
        mapperTable.put("derivate", MCRImportDerivateMapper.class);
    }

    /**
     * Use this method to add your own external resolver.
     * 
     * @param type type name of the resolver
     * @param resolver the resolver as class
     */
    public void addMapper(String type, Class<? extends MCRImportMapper> resolverClass) {
        mapperTable.put(type, resolverClass);
    }

    /**
     * This method creates a new instance of a mapper by the given
     * type. If the type is empty a new instance of
     * <code>MCRImportMetadataMapper</code> will be returned.
     * 
     * @param type the type of a mapper
     * @return a new instance of a mapper
     */
    public MCRImportMapper createMapperInstance(String type) throws IllegalAccessException, InstantiationException {
        Class<?> c = mapperTable.get(type);
        if(c == null)
            return null;
        Object o = c.newInstance();
        if(o instanceof MCRImportMapper) {
            return (MCRImportMapper)o;
        }
        return null;
    }
}