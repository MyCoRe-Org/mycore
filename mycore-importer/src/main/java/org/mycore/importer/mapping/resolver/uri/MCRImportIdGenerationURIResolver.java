package org.mycore.importer.mapping.resolver.uri;

import java.util.Hashtable;

/**
 * The id generation resolver creates increasing ids for
 * the given type. The type is set behind the colon of the
 * uri. Each type and his current id count are managed by a
 * hashtable.
 * 
 * @author Matthias Eichner
 */
public class MCRImportIdGenerationURIResolver implements MCRImportURIResolver {

    private Hashtable<String, Integer> idCounterTable = new Hashtable<String, Integer>();    

    public String resolve(String uri, String oldValue) {
        String key = uri.substring(uri.indexOf(":"));
        
        Integer currentCount = idCounterTable.get(key);
        if(currentCount == null)
            currentCount = 0;
        else
            currentCount++;
        idCounterTable.put(key, currentCount);
        
        return oldValue + currentCount;
    }

}