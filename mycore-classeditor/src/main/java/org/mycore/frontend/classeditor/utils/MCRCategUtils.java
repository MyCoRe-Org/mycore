package org.mycore.frontend.classeditor.utils;

import java.util.Set;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.frontend.classeditor.json.MCRJSONCategory;


public class MCRCategUtils{
    public static MCRCategory newCategory(MCRCategoryID id, Set<MCRLabel> labels, MCRCategoryID mcrCategoryID) {
        MCRJSONCategory category = new MCRJSONCategory();
        category.setId(id);
        category.setLabels(labels);
        category.setParentID(mcrCategoryID);
    
        return category;
    }

    public static String maskCategID(MCRCategoryID categoryID) {
        String rootID = categoryID.getRootID();
        String id = categoryID.getID();
        
        return rootID + "." + (id == null? "" : id);
    }
}