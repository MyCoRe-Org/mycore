package org.mycore.solr.index.strategy;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.mycore.common.config.MCRConfiguration;

/**
 * Checks if the file size is not too large.
 * 
 * @author Matthias Eichner
 * @author sherman
 * @author THomas Scheffler (yagee)
 */
public class MCRSolrFileSizeStrategy implements MCRSolrFileStrategy {

    /** the Threshold in bytes */
    public final static long OVER_THE_WIRE_THRESHOLD = MCRConfiguration.instance().getLong(
        CONFIG_PREFIX + "FileSizeStrategy.ThresholdInMegaBytes") * 1024 * 1024;

    @Override
    public boolean check(Path file, BasicFileAttributes attrs) {
        return attrs.size() <= OVER_THE_WIRE_THRESHOLD;
    }

}
