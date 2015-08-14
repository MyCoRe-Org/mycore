package org.mycore.wcms2.navigation;

import org.jdom2.Element;

import com.google.gson.JsonArray;

/**
 * 
 * @author Matthias Eichner
 */
public interface MCRWCMSSectionProvider extends MCRWCMSJSONProvider<Element, JsonArray> {

    public final static String JSON_TITLE = "title";

    public final static String JSON_LANG = "lang";

    public final static String JSON_DATA = "data";

    /**
     * 
     */
    @Override
    public Element fromJSON(JsonArray jsonSection);

    /**
     * Converts a MyCoRe Webpage to a json array. The array contains
     * all the section elements of the webpage including their content.
     * <p>
     * [<br> 
     *   {title: "title of section", lang: "de", data: "&lt;xml&gt;content of section&lt;/xml&gt;"},<br> 
     *   {title: "title of section 2", lang: "en", data: "&lt;xml&gt;content of section 2&lt;/xml&gt;"}<br> 
     * ]
     * </p>
     */
    @Override
    public JsonArray toJSON(Element object);

}
