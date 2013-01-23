/**
 * 
 */
package org.mycore.datamodel.classifications;

import java.util.Collection;
import java.util.Set;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

class MCRCategoryTools {
    static Element getCategoryElement(MCRCategory category, boolean withCounter, int numberObjects) {
        Element ce = new Element("category");
        Set<MCRLabel> labels = category.getLabels();
        ce.setAttribute("ID", category.getId().getID());
        if (withCounter) {
            ce.setAttribute("counter", Integer.toString(numberObjects));
        }

        for (MCRLabel label : labels) {
            ce.addContent(getElement(label));
        }
        for (MCRCategory child : category.getChildren()) {
            ce.addContent(getCategoryElement(child, withCounter, numberObjects));
        }
        return ce;
    }

    private static Element getElement(MCRLabel label) {
        Element le = new Element("label");
        if (stringNotEmpty(label.getLang())) {
            le.setAttribute("lang", label.getLang(), Namespace.XML_NAMESPACE);
        }
        if (stringNotEmpty(label.getText())) {
            le.setAttribute("text", label.getText());
        }
        if (stringNotEmpty(label.getDescription())) {
            le.setAttribute("description", label.getDescription());
        }
        return le;
    }

    private static boolean stringNotEmpty(String test) {
        return test != null && test.length() > 0;
    }
    
    /**
     * Get a category from the given id.
     * 
     * @param parent
     *      search for the category from this subtree
     * @param id
     *      search for the category with this id
     * @return
     *      the category if found, null otherwise
     */
    static MCRCategory findCategory(MCRCategory parent, String id) {
        return findCategory(parent, new MCRCategoryID(parent.getId().getRootID(), id));
    }
    
    static MCRCategory findCategory(MCRCategory parent, MCRCategoryID id) {
        for (MCRCategory cat : parent.getChildren()) {
            if (cat.getId().equals(id)) {
                return cat;
            }
            
            cat = findCategory(cat, id);
            if (cat != null) {
                return cat;
            }
        }

        return null;
    }
}