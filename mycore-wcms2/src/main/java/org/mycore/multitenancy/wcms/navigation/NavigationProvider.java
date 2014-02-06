package org.mycore.multitenancy.wcms.navigation;

import org.mycore.datamodel.navigation.Navigation;

import com.google.gson.JsonObject;

/**
 * Connection between the Navigation Datamodel and the WCMS.
 *
 * @author Matthias Eichner
 */
public interface NavigationProvider extends JSONProvider<Navigation, JsonObject> {

    public final static String JSON_HIERARCHY = "hierarchy";
    public final static String JSON_ITEMS = "items";
    public final static String JSON_CHILDREN = "children";
    public final static String JSON_WCMS_ID = "wcmsId";
    public final static String JSON_WCMS_TYPE = "wcmsType";

    public enum WCMSType {
        root, item, insert, menu
    }

    /**
     * <p>
     * Converts a <code>Navigation<code> object to a json one. The structure of the json is:
     * </p>
     * <p>
     * {<br />
     *   hierarchy: [<br />
     *     {"wcmsId":0,"children":[<br />
     *       {"wcmsId":1,"children":[<br />
     *         {"wcmsId":2}<br />
     *       ]}<br />
     *     ]}<br />
     *   }<br />
     *   items: [<br />
     *     {"wcmsId" : "0", "wcmsType" : "root", "mainTitle" : "My Main Title", "dir" : "/content" ...},<br />
     *     {"wcmsId" : "1", "wcmsType" : "menu", "id" : "main", "labelMap":{"de":"Hauptmenü links","en":"Main menu left"} ... }<br />
     *     {"wcmsId" : "2", "wcmsType" : "item", "type" : "intern", "style" : "bold" ...}<br />
     *     ...<br />
     *   ]<br />
     * }
     * </p>
     * @param navigation
     * @return the generated json
     */
    @Override
    public JsonObject toJSON(Navigation navigation);

    /**
     * Converts an WCMS JSON Object to an <code>Navigation</code> object.
     * 
     * TODO: json data structure
     * 
     * @param navigation
     * @return
     */
    @Override
    public Navigation fromJSON(JsonObject jsonNavigation);

}
