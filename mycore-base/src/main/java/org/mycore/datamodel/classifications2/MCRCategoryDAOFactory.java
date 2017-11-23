package org.mycore.datamodel.classifications2;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
public class MCRCategoryDAOFactory {

    private static final String STANDARD_DAO = MCRCategoryDAOImpl.class.getCanonicalName();

    private static MCRCategoryDAO INSTANCE;

    static {
        INSTANCE = MCRConfiguration.instance().getInstanceOf("MCR.Category.DAO", STANDARD_DAO);
    }

    /**
     * Returns an instance of a MCRCategoryDAO implementator.
     */
    public static MCRCategoryDAO getInstance() {
        return INSTANCE;
    }

    /**
     * Sets a new category dao implementation for this factory. This could be useful for different test cases
     * with mock objects.
     * 
     * @param daoClass new dao class
     */
    public static synchronized void set(Class<? extends MCRCategoryDAO> daoClass) throws IllegalAccessException,
        InstantiationException {
        INSTANCE = daoClass.newInstance();
    }

}
