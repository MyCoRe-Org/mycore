package org.mycore.solr.index.strategy;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRFile;

/**
 * @author Matthias Eichner
 */
public class MCRSolrIndexStrategyManager {

    private static final MCRSolrFileStrategy FILE_STRATEGY;

    static {
        FILE_STRATEGY = MCRConfiguration.instance().getInstanceOf(CONFIG_PREFIX + "FileIndexStrategy",
            MCRSolrFileSizeStrategy.class.getCanonicalName(), MCRSolrFileStrategy.class);
    }

    public static boolean checkFile(MCRFile file) {
        return FILE_STRATEGY.check(file);
    }

}
