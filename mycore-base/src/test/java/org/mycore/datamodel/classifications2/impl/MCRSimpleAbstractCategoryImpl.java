package org.mycore.datamodel.classifications2.impl;

import java.util.HashSet;
import java.util.List;

import org.mycore.datamodel.classifications2.MCRCategory;

final class MCRSimpleAbstractCategoryImpl extends MCRAbstractCategoryImpl {
    {
        labels = new HashSet<>();
    }

    public MCRSimpleAbstractCategoryImpl() {
        super();
    }

    @Override
    protected void setChildrenUnlocked(List<MCRCategory> children) {
    }

    @Override
    public int getLevel() {
        return 0;
    }
}
