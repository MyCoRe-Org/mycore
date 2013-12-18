package org.mycore.solr.index.strategy;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRFile;

/**
 * Checks if the file size is not too large.
 * 
 * @author Matthias Eichner
 * @author sherman
 */
public class MCRSolrFileSizeStrategy implements MCRSolrFileStrategy {

    /** the Threshold in bytes */
    public final static long OVER_THE_WIRE_THRESHOLD = MCRConfiguration.instance().getLong(
        CONFIG_PREFIX + "FileSizeStrategy.ThresholdInMegaBytes") * 1024 * 1024;

    public boolean check(MCRFile file) {
        return file.getSize() <= OVER_THE_WIRE_THRESHOLD;
    }

}
