package org.mycore.wcms2.navigation;

import org.mycore.wcms2.datamodel.MCRNavigation;

import com.google.gson.JsonObject;

/**
 * Connection between the Navigation Datamodel and the WCMS.
 *
 * @author Matthias Eichner
 */
public interface MCRWCMSNavigationProvider extends MCRWCMSJSONProvider<MCRNavigation, JsonObject> {

    String JSON_HIERARCHY = "hierarchy";

    String JSON_ITEMS = "items";

    String JSON_CHILDREN = "children";

    String JSON_WCMS_ID = "wcmsId";

    String JSON_WCMS_TYPE = "wcmsType";

    enum WCMSType {
        root, item, insert, menu, group
    }

    /**
     * <p>
     * Converts a <code>Navigation</code> object to a json one. The structure of the json is:
     * </p>
     * <pre>
     * {
     *   hierarchy: [
     *     {"wcmsId":0,"children":[
     *       {"wcmsId":1,"children":[
     *         {"wcmsId":2}
     *       ]}
     *     ]}
     *   }
     *   items: [
     *     {"wcmsId" : "0", "wcmsType" : "root", "mainTitle" : "My Main Title", "dir" : "/content" ...},
     *     {"wcmsId" : "1", "wcmsType" : "menu", "id" : "main", "labelMap":{"de":"Hauptmenü links","en":"Main menu left"} ... }
     *     {"wcmsId" : "2", "wcmsType" : "item", "type" : "intern", "style" : "bold" ...}
     *     ...
     *   ]
     * }
     * </pre>
     * @return the generated json
     */
    @Override JsonObject toJSON(MCRNavigation navigation);

    /**
     * Converts an WCMS JSON Object to an <code>Navigation</code> object.
     * 
     * TODO: json data structure
     */
    @Override MCRNavigation fromJSON(JsonObject jsonNavigation);

}
