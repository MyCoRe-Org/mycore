package org.mycore.services.acl.filter;

import java.util.Properties;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * Filters a list of creators. They could be seperated by a comma.
 * 
 * @author Matthias Eichner
 */
public class MCRAclCreatorFilter implements MCRAclCriterionFilter {

    public Criterion filter(Properties properties) {
        String creator = properties.getProperty("creator");
        if(creator != null && !creator.equals("")) {
            String[] creatorFilter = creator.split(",");
            return Restrictions.in("creator", creatorFilter);
        }
        return null;
    }

}