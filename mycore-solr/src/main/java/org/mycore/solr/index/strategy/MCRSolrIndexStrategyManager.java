package org.mycore.solr.index.strategy;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.mycore.common.config.MCRConfiguration;

/**
 * @author Matthias Eichner
 * @author Thomas Scheffler (yagee)
 */
public class MCRSolrIndexStrategyManager {

    private static final MCRSolrFileStrategy FILE_STRATEGY;

    static {
        FILE_STRATEGY = MCRConfiguration.instance().<MCRSolrFileStrategy> getInstanceOf(
            CONFIG_PREFIX + "FileIndexStrategy", (String) null);
    }

    public static boolean checkFile(Path file, BasicFileAttributes attrs) {
        return FILE_STRATEGY.check(file, attrs);
    }

}
