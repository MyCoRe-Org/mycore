package org.mycore.services.acl.filter;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * Filters a permission (read, create-user, writedb...).
 * 
 * @author Matthias Eichner
 */
public class MCRAclPermissionFilter implements MCRAclCriterionFilter {

    private static Logger LOGGER = Logger.getLogger(MCRAclPermissionFilter.class);

    public static final String PROPERTY_NAME = "acpool";
    
    public Criterion filter(HttpServletRequest request) {
        String permissionFilter = MCRServlet.getProperty(request, PROPERTY_NAME);

        if (permissionFilter != null && !permissionFilter.equals("")) {
            LOGGER.info("ACPOOL Filter: " + permissionFilter + "\t" + permissionFilter.replaceAll("\\*", "%"));
            return Restrictions.like("key.acpool", permissionFilter.replaceAll("\\*", "%"));
        }
        return null;
    }

}