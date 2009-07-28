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
public abstract class MCRImportMetadataResolverTable {

    private static final Logger LOGGER = Logger.getLogger(MCRImportMetadataResolverTable.class);
    
    private static ArrayList<ResolverData<?>> resolverTable;

    static {
        resolverTable = new ArrayList<ResolverData<?>>();
        resolverTable.add( new ResolverData<MCRImportTextResolver>("text", "MCRMetaLangText", MCRImportTextResolver.class) );
        resolverTable.add( new ResolverData<MCRImportBooleanResolver>("boolean", "MCRMetaBoolean", MCRImportBooleanResolver.class) );
        resolverTable.add( new ResolverData<MCRImportClassificationResolver>("classification", "MCRMetaClassification", MCRImportClassificationResolver.class) );
        resolverTable.add( new ResolverData<MCRImportISODataResolver>("date", "MCRMetaISO8601Date", MCRImportISODataResolver.class) );
        resolverTable.add( new ResolverData<MCRImportLinkIDResolver>("link", "MCRMetaLinkID", MCRImportLinkIDResolver.class) );
        resolverTable.add( new ResolverData<MCRImportLinkResolver>("href", "MCRMetaLink", MCRImportLinkResolver.class) );
        resolverTable.add( new ResolverData<MCRImportDerivateLinkResolver>("derlink", "MCRMetaDerivateLink", MCRImportDerivateLinkResolver.class) );
        resolverTable.add( new ResolverData<MCRImportMetaXMLResolver>("xml", "MCRMetaXML", MCRImportMetaXMLResolver.class) );
        resolverTable.add( new ResolverData<MCRImportNumberResolver>("number", "MCRMetaNumber", MCRImportNumberResolver.class) );
    }

    /**
     * Adds resolver to the table.
     * 
     * @param type the type of the resolver, in general this is a shortcut
     * @param className the MyCoRe-classname which this resolver is connected to
     * @param resolverClass the real java implementation of the resolver
     */
    @SuppressWarnings("unchecked")
    public static void addToResolverTable(String type, String className, Class<?> resolverClass) {
        resolverTable.add( new ResolverData(type, className, resolverClass) );
    }

    /**
     * Returns a MyCoRe classname by the given type.
     * 
     * @param type the type of the resolver
     * @return the classname
     */
    public static String getClassNameByType(String type) {
        for(ResolverData<?> rd : resolverTable) {
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
    public static String getTypeByClassName(String className) {
        for(ResolverData<?> rd : resolverTable) {
            if(className.equals(rd.getClassName()))
                return rd.getType();
        }
        return null;
    }

    /**
     * Tries to resolve the given type to create an MCRImportMetadataResolver 
     * instance. If no class for this type is defined, null will be returned.
     * 
     * @param type
     * @return
     */
    public static MCRImportMetadataResolver createInstance(String type) {
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
    protected static Class<?> getResolverClassByType(String type) {
        for(ResolverData<?> rd : resolverTable) {
            if(type.equals(rd.getType()))
                return rd.getResolverClass();
        }
        return null;
    }

    /**
     * Internal class to represent a resolver by its type,
     * MyCoRe classname and the java class.
     */
    private static class ResolverData<T extends MCRImportMetadataResolver> {
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