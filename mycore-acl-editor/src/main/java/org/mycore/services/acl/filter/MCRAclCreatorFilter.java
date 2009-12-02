package org.mycore.services.acl.filter;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * Filters a list of creators. They could be seperated by a comma.
 * 
 * @author Matthias Eichner
 */
public class MCRAclCreatorFilter implements MCRAclCriterionFilter {

    public Criterion filter(HttpServletRequest request) {
        String creator = MCRServlet.getProperty(request, "creator");
        if(creator != null && !creator.equals("")) {
            String[] creatorFilter = creator.split(",");
            return Restrictions.in("creator", creatorFilter);
        }
        return null;
    }

}