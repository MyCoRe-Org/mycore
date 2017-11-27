/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.wcms2.navigation;

import org.jdom2.Element;

import com.google.gson.JsonArray;

/**
 * 
 * @author Matthias Eichner
 */
public interface MCRWCMSSectionProvider extends MCRWCMSJSONProvider<Element, JsonArray> {

    String JSON_TITLE = "title";

    String JSON_LANG = "lang";

    String JSON_DATA = "data";

    /**
     * 
     */
    @Override
    Element fromJSON(JsonArray jsonSection);

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
    JsonArray toJSON(Element object);

}
