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

package org.mycore.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.mycore.services.i18n.MCRTranslation;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRJSONUtils {

    public static JsonArray getJsonArray(Collection<String> list) {
        JsonArray ja = new JsonArray();
        for (String s : list) {
            ja.add(new JsonPrimitive(s));
        }
        return ja;
    }

    public static String getTranslations(String prefixes, String lang) {
        Map<String, String> transMap = new HashMap<>();

        Arrays.stream(prefixes.split(","))
            .map(currentPrefix -> MCRTranslation.translatePrefix(currentPrefix.substring(0, currentPrefix.length() - 1),
                MCRTranslation.getLocale(lang)))
            .forEach(transMap::putAll);

        Gson gson = new Gson();
        return gson.toJson(transMap);
    }

}
