package org.mycore.importer.mapping.resolver.uri;

import java.util.Hashtable;

import org.apache.log4j.Logger;

/**
 * This class contains a table of uri resolvers. In general
 * the uri resolvers are automatically added by the
 * <code>MCRImportMappingManager</code>. To resolve a uri call
 * the method resolveURI.
 * 
 * @author Matthias Eichner
 */
public class MCRImportURIResolverMananger {

    private static final Logger LOGGER = Logger.getLogger(MCRImportURIResolverMananger.class);
    
    /**
     * Table of all uris. The key is the prefix and the value
     * an instance of MCRImportURIResolver.
     */
    private Hashtable<String, MCRImportURIResolver> supportedSchemes;

    /**
     * Creates a new instance to handle uris. For each import is only
     * one <code>MCRImportURIResolverMananger</code> is required. This
     * instance could be get by calling
     * <code>MCRImportMappingManager.getInstance().getURIResolverManager()</code>.
     * So it is in general not necessary, to create a second instance of this class.
     */
    public MCRImportURIResolverMananger() {
        supportedSchemes = new Hashtable<String, MCRImportURIResolver>();
        init();
    }

    /**
     * Adds some default uri with their resolvers to the table.
     */
    public void init() {
        supportedSchemes.put("idGen", new MCRImportIdGenerationURIResolver());
        supportedSchemes.put("gerDate", new MCRImportGermanDateURIResolver());
    }

    /**
     * Add a new uri resolver.
     * 
     * @param uriPrefix the prefix of the resolver
     * @param resolver the uri resolver as an instance of <code>MCRImportURIResolver</code>
     */
    public void addURIResolver(String uriPrefix, MCRImportURIResolver resolver) {
        supportedSchemes.put(uriPrefix, resolver);
    }

    /**
     * Removes an uri resolver by its prefix.
     * 
     * @param uriPrefix the prefix of the uri resolver
     */
    public void removeURIResolver(String uriPrefix) {
        supportedSchemes.remove(uriPrefix);
    }

    /**
     * This method tries to resolve an uri. It pass through the hash table
     * to get the correct <code>MCRImportURIResolver</code>. If found,
     * the method resolve is called.
     * 
     * @param uri the uri in the form prefix:urivalue
     * @param oldValue a source value which have to resolved
     * @return the new resolved value
     */
    public String resolveURI(String uri, String oldValue) {
        String prefix = uri;
        int splitIndex = uri.indexOf(":");
        if(splitIndex != -1)
            prefix = uri.substring(0, splitIndex);
        MCRImportURIResolver resolver = supportedSchemes.get(prefix);
        if(resolver == null)
            LOGGER.error("Could not find URI resolver " + uri);
        return resolver.resolve(uri, oldValue);
    }

    /**
     * Call this method only if you are certain that you no longer need
     * the internal hash table.
     */
    public void destroy() {
        supportedSchemes.clear();
    }

}
