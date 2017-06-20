package org.mycore.mods.identifier;

import java.util.Map;
import java.util.Optional;

/**
 * Identifies identifiers in specific sources. E.g. T=URL could Detect the <b>GND <i>118948032</i></b> in a URL like <a href="http://d-nb.info/gnd/118948032">http://d-nb.info/gnd/118948032</a>.
 */
public interface MCRIdentifierDetector<T> {
    /**
     * @param resolvable some thing that can be resolved to a unique identifier
     * @return a {@link java.util.Map.Entry} with the identifier type as key and the identifier as value. The Optional can be empty if no identifier can be detected or if a error occurs.
     */
    Optional<Map.Entry<String, String>> detect(T resolvable);
}
