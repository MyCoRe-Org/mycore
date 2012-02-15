package org.mycore.services.acl.filter;

import java.util.Properties;

import org.hibernate.criterion.Criterion;

public interface MCRAclCriterionFilter {

    public Criterion filter(Properties properties);

}