package org.mycore.services.acl.filter;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.criterion.Criterion;

public interface MCRAclCriterionFilter {

    public Criterion filter(HttpServletRequest request);

}