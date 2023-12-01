package org.mycore.common.log4j2.filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.ParameterizedMessage;

/**
 * Detects duplicate instances of logging events that have the same parameters for a given format.
 * Uses an LRU cache to only filters up to a maximum amount of parameter sets per format. The cache size per
 * format con be configured with the <code>cacheSizePerFormat</code> configuration value; defaults to
 * <code>1000</code>.
 * <p>
 * By default, the result of a match is <code>DENY</code> and on a mismatch is <code>NEUTRAL</code>.  
 */
@Plugin(name = "MCRUniqueFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE)
public class MCRUniqueFilter extends AbstractFilter {

    private final int cacheSizePerFormat;

    private final Map<String, Set<List<Object>>> cacheByFormat = new HashMap<>();

    private MCRUniqueFilter(int cacheSizePerFormat, Result onMatch, Result onMismatch) {
        super(onMatch, onMismatch);
        this.cacheSizePerFormat = cacheSizePerFormat;
    }

    @Override
    public Result filter(LogEvent event) {
        if (event.getMessage() instanceof ParameterizedMessage message) {
            List<Object> parameters = Arrays.asList(message.getParameters());
            Set<List<Object>> cache = getCache(message.getFormat());
            if (cache.contains(parameters)) {
                return onMatch;
            } else {
                cache.add(parameters);
            }
        }
        return onMismatch;
    }

    private Set<List<Object>> getCache(String format) {
        return cacheByFormat.computeIfAbsent(format, f -> createCache());
    }

    private Set<List<Object>> createCache() {
        return Collections.newSetFromMap(new LinkedHashMap<>() {

            @Override
            protected boolean removeEldestEntry(Map.Entry<List<Object>, Boolean> eldest) {
                return size() > cacheSizePerFormat;
            }

        });
    }

    @PluginFactory
    public static MCRUniqueFilter createFilter(
        @PluginAttribute(value = "cacheSizePerFormat", defaultInt = 1000) int cacheSizePerFormat,
        @PluginAttribute("onMatch") Result match, @PluginAttribute("onMismatch") Result mismatch) {
        Result onMatch = match == null ? Result.DENY : match;
        Result onMismatch = mismatch == null ? Result.NEUTRAL : mismatch;
        return new MCRUniqueFilter(cacheSizePerFormat, onMatch, onMismatch);
    }

}
