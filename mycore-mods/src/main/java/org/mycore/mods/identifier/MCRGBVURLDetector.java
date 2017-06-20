package org.mycore.mods.identifier;

import java.net.URI;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * This Detector normalizes <a href="http://uri.gbv.de/document/gvk:ppn:834532662">http://uri.gbv.de/document/gvk:ppn:834532662</a> to URI <a href="gvk:ppn:834532662">gvk:ppn:834532662</a>.
 * <p>
 * TODO: maybe detect other identifier then ppn
 */
public class MCRGBVURLDetector implements MCRIdentifierDetector<URI> {

    public static final String GBV_PREFIX = "uri.gbv.de/document/";

    public static final String GVK_PREFIX = "gso.gbv.de/DB=2.1/PPNSET?PPN=";

    @Override
    public Optional<Map.Entry<String, String>> detect(URI resolvable) {
        String urlString = resolvable.toString();

        // case http://uri.gbv.de/document/
        if (urlString.contains(GBV_PREFIX)) {
            String[] strings = urlString.split(GBV_PREFIX, 2);
            if (strings.length == 2) {
                String ppnIdentifier = strings[1];
                String[] ppnValues = ppnIdentifier.split(":"); // format is $catalog:ppn:$ppn
                if (ppnValues.length == 3 && ppnValues[1].equals("ppn")) {
                    return Optional.of(new AbstractMap.SimpleEntry<>("ppn", ppnIdentifier));
                }
            }
        }

        if (urlString.contains(GVK_PREFIX)) {
            String[] strings = urlString.split(Pattern.quote(GVK_PREFIX), 2);
            if (strings.length == 2) {
                String gvkPPN = strings[1];
                return Optional.of(new AbstractMap.SimpleEntry<>("ppn", "gvk:ppn:" + gvkPPN));
            }
        }

        return Optional.empty();
    }
}
