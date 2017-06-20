package org.mycore.frontend.classeditor.wrapper;

import java.util.List;
import java.util.Map;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;

public class MCRCategoryListWrapper {

    private List<MCRCategory> categList;

    private Map<MCRCategoryID, Boolean> linkMap = null;

    public MCRCategoryListWrapper(List<MCRCategory> categList) {
        this.categList = categList;
    }

    public MCRCategoryListWrapper(List<MCRCategory> categList, Map<MCRCategoryID, Boolean> linkMap) {
        this.categList = categList;
        this.linkMap = linkMap;
    }

    public List<MCRCategory> getList() {
        return categList;
    }

    public Map<MCRCategoryID, Boolean> getLinkMap() {
        return linkMap;
    }
}
