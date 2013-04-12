package org.mycore.solr.index.strategy;

import org.mycore.common.MCRConfiguration;
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
            "MCR.Module-solr.OverTheWireThresholdInMegaBytes", 32) * 1024 * 1024;

    public boolean check(MCRFile file) {
        return file.getSize() <= OVER_THE_WIRE_THRESHOLD;
    }

}
