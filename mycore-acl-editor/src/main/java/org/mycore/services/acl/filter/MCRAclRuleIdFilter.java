package org.mycore.services.acl.filter;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * Filters a rule id (e.g. SYSTEMRULE00000000004).
 * 
 * @author Matthias Eichner
 */
public class MCRAclRuleIdFilter implements MCRAclCriterionFilter {

    private static Logger LOGGER = Logger.getLogger(MCRAclRuleIdFilter.class);

    public Criterion filter(Properties properties) {
        String ridFilter = properties.getProperty("rid");

        if (ridFilter != null && !ridFilter.equals("")) {
            LOGGER.info("RID Filter: " + ridFilter);
            return Restrictions.like("rule.rid", ridFilter.replaceAll("\\*", "%"));
        }
        return null;
    }

}