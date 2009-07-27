package org.mycore.importer.mapping.resolver.uri;

import java.util.Hashtable;

import org.apache.log4j.Logger;

/**
 * This singleton contains a table of import uri resolvers.
 * In general the uri resolvers are automatically added by
 * the mapping configuration class. To resolve a uri call
 * the method resolveURI.
 * 
 * @author Matthias Eichner
 */
public class MCRImportURIResolverMananger {

    private static final Logger LOGGER = Logger.getLogger(MCRImportURIResolverMananger.class);
    
    private static MCRImportURIResolverMananger INSTANCE;
    private Hashtable<String, MCRImportURIResolver> supportedSchemes;
    
    private MCRImportURIResolverMananger() {
        supportedSchemes = new Hashtable<String, MCRImportURIResolver>();
        init();
    }

    public static MCRImportURIResolverMananger getInstance() {
        if(INSTANCE == null)
            INSTANCE = new MCRImportURIResolverMananger();
        return INSTANCE;
    }

    public void init() {
        supportedSchemes.put("idGen", new MCRImportIdGenerationURIResolver());
        supportedSchemes.put("gerDate", new MCRImportGermanDateURIResolver());
    }

    public void addURIResolver(String uriPrefix, MCRImportURIResolver resolver) {
        supportedSchemes.put(uriPrefix, resolver);
    }
    public void removeURIResolver(String uriPrefix) {
        supportedSchemes.remove(uriPrefix);
    }

    public String resolveURI(String uri, String oldValue) {
        String prefix = uri;
        int splitIndex = uri.indexOf(":");
        if(splitIndex != -1)
            prefix = uri.substring(0, splitIndex - 1);
        MCRImportURIResolver resolver = supportedSchemes.get(prefix);
        if(resolver == null)
            LOGGER.error("Could not find URI resolver " + uri);
        return resolver.resolve(uri, oldValue);
    }

    /**
     * Call this method only if you are certain that you no longer need
     * this singleton.
     */
    public void destroy() {
        supportedSchemes.clear();
        INSTANCE = null;
    }

}
