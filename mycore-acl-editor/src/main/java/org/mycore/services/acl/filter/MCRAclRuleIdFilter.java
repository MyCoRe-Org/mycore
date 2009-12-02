package org.mycore.services.acl.filter;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * Filters a rule id (e.g. SYSTEMRULE00000000004).
 * 
 * @author Matthias Eichner
 */
public class MCRAclRuleIdFilter implements MCRAclCriterionFilter {

    private static Logger LOGGER = Logger.getLogger(MCRAclRuleIdFilter.class);

    public Criterion filter(HttpServletRequest request) {
        String ridFilter = MCRServlet.getProperty(request, "rid");

        if (ridFilter != null && !ridFilter.equals("")) {
            LOGGER.info("RID Filter: " + ridFilter);
            return Restrictions.like("rule.rid", ridFilter.replaceAll("\\*", "%"));
        }
        return null;
    }

}