/*
 * $Revision: 29635 $ $Date: 2014-04-10 10:55:06 +0200 (Do, 10 Apr 2014) $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.restapi.v1.errors;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * stores error informations during REST requests
 * 
 * @author Robert Stephan
 * 
 * @version $Revision: $ $Date: $
 */
public class MCRRestAPIError {
    public static final String CODE_WRONG_PARAMETER = "WRONG_PARAMETER";

    public static final String CODE_WRONG_QUERY_PARAMETER = "WRONG_QUERY_PARAMETER";

    public static final String CODE_WRONG_ID = "WRONG_ID";

    public static final String CODE_NOT_FOUND = "NOT_FOUND";

    public static final String CODE_INTERNAL_ERROR = "INTERNAL_ERROR";

    public static final String CODE_ACCESS_DENIED = "ACCESS_DENIED";

    public static final String CODE_INVALID_AUTHENCATION = "INVALID_AUTHENTICATION";

    public static final String CODE_INVALID_DATA = "INVALID_DATA";

    String code = "";

    String title = "";

    String detail = null;

    public MCRRestAPIError(String code, String title, String detail) {
        this.code = code;
        this.title = title;
        this.detail = detail;
    }

    public JsonObject toJsonObject() {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("title", title);
        if (detail != null) {
            error.addProperty("detail", detail);
        }
        return error;
    }

    public String toJSONString() {
        JsonArray errors = new JsonArray();
        errors.add(toJsonObject());
        JsonObject errorMsg = new JsonObject();
        errorMsg.add("errors", errors);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(errorMsg);
    }

    public static String convertErrorListToJSONString(List<MCRRestAPIError> errors) {
        JsonArray jaErrors = new JsonArray();
        for (MCRRestAPIError e : errors) {
            jaErrors.add(e.toJsonObject());
        }

        JsonObject errorMsg = new JsonObject();
        errorMsg.add("errors", jaErrors);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(errorMsg);
    }
}
