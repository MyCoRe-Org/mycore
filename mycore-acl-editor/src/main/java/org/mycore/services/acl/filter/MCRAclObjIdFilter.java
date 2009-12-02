package org.mycore.services.acl.filter;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * Filters a mcr object id (e.g. DocPortal_author_000000001).
 * 
 * @author Matthias Eichner
 */
public class MCRAclObjIdFilter implements MCRAclCriterionFilter {

    private static Logger LOGGER = Logger.getLogger(MCRAclObjIdFilter.class);

    public static final String PROPERTY_NAME = "objid";

    public Criterion filter(HttpServletRequest request) {
        String objidFilter = MCRServlet.getProperty(request, PROPERTY_NAME);

        if (objidFilter != null && !objidFilter.equals("")) {
            LOGGER.info("OBJID Filter: " + objidFilter + "\t" + objidFilter.replaceAll("\\*", "%"));
            return Restrictions.like("key.objid", objidFilter.replaceAll("\\*", "%"));
        }
        return null;
    }

}