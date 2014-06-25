/**
 * 
 */
package org.mycore.datamodel.classifications2.impl;

import java.util.HashSet;
import java.util.List;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRLabel;

final class MCRSimpleAbstractCategoryImpl extends MCRAbstractCategoryImpl {
    {
        labels=new HashSet<MCRLabel>();
    }
    
    public MCRSimpleAbstractCategoryImpl() {
        super();
    }

    @Override
    protected void setChildren(List<MCRCategory> children) {
    }

    public int getLevel() {
        return 0;
    }
}
