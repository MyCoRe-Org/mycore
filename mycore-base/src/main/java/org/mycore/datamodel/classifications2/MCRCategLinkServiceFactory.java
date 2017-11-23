package org.mycore.datamodel.classifications2;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
public class MCRCategLinkServiceFactory {
    private static final String STANDARD_IMPL = MCRCategLinkServiceImpl.class.getCanonicalName();

    private static MCRCategLinkService instance = MCRConfiguration.instance()
        .getInstanceOf("MCR.Category.LinkService", STANDARD_IMPL);

    /**
     * Returns an instance of a MCRCategoryDAO implementator.
     */
    public static MCRCategLinkService getInstance() {
        return instance;
    }

}
