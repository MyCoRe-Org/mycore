package org.mycore.importer.mapping.resolver.uri;

/**
 * Use this interface to create your own logic for
 * resolving mapping values.
 * 
 * @author Matthias Eichner
 */
public interface MCRImportURIResolver {

    /**
     * Tries to resolve the oldValue.
     * 
     * @param uri the uri as string
     * @param oldValue the source value
     * @return the new resolved value
     */
    public String resolve(String uri, String oldValue);

}