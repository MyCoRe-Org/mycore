package org.mycore.services.acl.filter;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * Filters a mcr object id (e.g. DocPortal_author_000000001).
 * 
 * @author Matthias Eichner
 */
public class MCRAclObjIdFilter implements MCRAclCriterionFilter {

    private static Logger LOGGER = Logger.getLogger(MCRAclObjIdFilter.class);

    public static final String PROPERTY_NAME = "objid";

    public Criterion filter(Properties properties) {
        String objidFilter = properties.getProperty(PROPERTY_NAME);

        if (objidFilter != null && !objidFilter.equals("")) {
            LOGGER.info("OBJID Filter: " + objidFilter + "\t" + objidFilter.replaceAll("\\*", "%"));
            return Restrictions.like("key.objid", objidFilter.replaceAll("\\*", "%"));
        }
        return null;
    }

}