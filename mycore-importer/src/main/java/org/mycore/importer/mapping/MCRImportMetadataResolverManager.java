package org.mycore.importer.mapping;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.mycore.importer.mapping.resolver.metadata.MCRImportBooleanResolver;
import org.mycore.importer.mapping.resolver.metadata.MCRImportClassificationResolver;
import org.mycore.importer.mapping.resolver.metadata.MCRImportDerivateLinkResolver;
import org.mycore.importer.mapping.resolver.metadata.MCRImportISODataResolver;
import org.mycore.importer.mapping.resolver.metadata.MCRImportLinkIDResolver;
import org.mycore.importer.mapping.resolver.metadata.MCRImportLinkResolver;
import org.mycore.importer.mapping.resolver.metadata.MCRImportMetaXMLResolver;
import org.mycore.importer.mapping.resolver.metadata.MCRImportMetadataResolver;
import org.mycore.importer.mapping.resolver.metadata.MCRImportNumberResolver;
import org.mycore.importer.mapping.resolver.metadata.MCRImportTextResolver;

/**
 * This class contains a list of all metadata resolvers.
 * A metadata resolver is defined by a type, a MyCoRe
 * classname and its java representation. To create an
 * instance of a metadata resolver you can call the method 
 * createInstanceByType(String type).
 * 
 * @author Matthias Eichner
 */
public class MCRImportMetadataResolverManager {

    private static final Logger LOGGER = Logger.getLogger(MCRImportMetadataResolverManager.class);

    /**
     * List of resolver data. A resolver data is defined
     * by a type, a classname, and class object of
     * <code>MCRImportMetadataResolver</code>.
     */
    private ArrayList<ResolverData<?>> resolverList;

    /**
     * Creates a new instance to handle metadata resolvers. For each import is only
     * one <code>MCRImportMetadataResolverManager</code> is required. This
     * instance could be get by calling
     * <code>MCRImportMappingManager.getInstance().getMetadataResolver()</code>.
     * So it is in general not necessary, to create a second instance of this class.
     */
    public MCRImportMetadataResolverManager() {
        addDefaultResolver();
    }

    /**
     * Adds all default metadata resolvers to the resolver list.
     */
    private void addDefaultResolver() {
        resolverList = new ArrayList<ResolverData<?>>();
        resolverList.add( new ResolverData<MCRImportTextResolver>("text", "MCRMetaLangText", MCRImportTextResolver.class) );
        resolverList.add( new ResolverData<MCRImportBooleanResolver>("boolean", "MCRMetaBoolean", MCRImportBooleanResolver.class) );
        resolverList.add( new ResolverData<MCRImportClassificationResolver>("classification", "MCRMetaClassification", MCRImportClassificationResolver.class) );
        resolverList.add( new ResolverData<MCRImportISODataResolver>("date", "MCRMetaISO8601Date", MCRImportISODataResolver.class) );
        resolverList.add( new ResolverData<MCRImportLinkIDResolver>("link", "MCRMetaLinkID", MCRImportLinkIDResolver.class) );
        resolverList.add( new ResolverData<MCRImportLinkResolver>("href", "MCRMetaLink", MCRImportLinkResolver.class) );
        resolverList.add( new ResolverData<MCRImportDerivateLinkResolver>("derlink", "MCRMetaDerivateLink", MCRImportDerivateLinkResolver.class) );
        resolverList.add( new ResolverData<MCRImportMetaXMLResolver>("xml", "MCRMetaXML", MCRImportMetaXMLResolver.class) );
        resolverList.add( new ResolverData<MCRImportNumberResolver>("number", "MCRMetaNumber", MCRImportNumberResolver.class) );
    }

    /**
     * Adds a new metadata resolver to the table.
     * 
     * @param type the type of the resolver, in general this is a shortcut
     * @param className the MyCoRe-classname which this resolver is connected to
     * @param resolverClass the real java implementation of the resolver
     */
    @SuppressWarnings("unchecked")
    public void addToResolverTable(String type, String className, Class<?> resolverClass) {
        resolverList.add( new ResolverData(type, className, resolverClass) );
    }

    /**
     * Returns a MyCoRe classname by the given type.
     * 
     * @param type the type of the resolver
     * @return the classname
     */
    public String getClassNameByType(String type) {
        for(ResolverData<?> rd : resolverList) {
            if(type.equals(rd.getType()))
                return rd.getClassName();
        }
        return null;
    }

    /**
     * Returns the type shortcut of the given MyCoRe classname.
     * 
     * @param className the classname of the resolver
     * @return the type shortcut
     */
    public String getTypeByClassName(String className) {
        for(ResolverData<?> rd : resolverList) {
            if(className.equals(rd.getClassName()))
                return rd.getType();
        }
        return null;
    }

    /**
     * Tries to resolve the given type to create an MCRImportMetadataResolver 
     * instance. If no class for this type is defined, null will be returned.
     * 
     * @param type the shortcut
     * @return a new instance of <code>MCRImportMetadataResolver</code>
     */
    public MCRImportMetadataResolver createInstance(String type) {
        Class<?> c = getResolverClassByType(type);
        try {
            Object o = c.newInstance();
            if(!(o instanceof MCRImportMetadataResolver)) {
                LOGGER.error("The resolver " + c.getSimpleName() + " is not an instance of MCRImportResolver!");
            }
            return (MCRImportMetadataResolver)o;
        } catch(IllegalAccessException iae) {
            LOGGER.error(iae);
        } catch(InstantiationException ie) {
            LOGGER.error(ie);
        }
        return null;
    }

    /**
     * Returns the java class object from the table by the
     * given type.
     */
    protected Class<?> getResolverClassByType(String type) {
        for(ResolverData<?> rd : resolverList) {
            if(type.equals(rd.getType()))
                return rd.getResolverClass();
        }
        return null;
    }

    /**
     * Internal class to represent a resolver by its type,
     * MyCoRe classname and the java class.
     */
    private class ResolverData<T extends MCRImportMetadataResolver> {
        private String type;
        private String className;
        private Class<T> resolverClass;
        
        public ResolverData(String type, String className, Class<T> resolverClass) {
            this.type = type;
            this.className = className;
            this.resolverClass = resolverClass;
        }
        public String getType() {
            return type;
        }
        public String getClassName() {
            return className;
        }
        public Class<T> getResolverClass() {
            return resolverClass;
        }
    }
}