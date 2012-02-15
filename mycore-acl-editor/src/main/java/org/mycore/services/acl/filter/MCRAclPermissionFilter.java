package org.mycore.services.acl.filter;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * Filters a permission (read, create-user, writedb...).
 * 
 * @author Matthias Eichner
 */
public class MCRAclPermissionFilter implements MCRAclCriterionFilter {

    private static Logger LOGGER = Logger.getLogger(MCRAclPermissionFilter.class);

    public static final String PROPERTY_NAME = "acpool";
    
    public Criterion filter(Properties properties) {
        String permissionFilter = properties.getProperty(PROPERTY_NAME);

        if (permissionFilter != null && !permissionFilter.equals("")) {
            LOGGER.info("ACPOOL Filter: " + permissionFilter + "\t" + permissionFilter.replaceAll("\\*", "%"));
            return Restrictions.like("key.acpool", permissionFilter.replaceAll("\\*", "%"));
        }
        return null;
    }

}