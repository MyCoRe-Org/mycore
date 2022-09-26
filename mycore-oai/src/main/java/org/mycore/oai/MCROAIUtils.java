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
package org.mycore.oai;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.oai.classmapping.MCRClassificationAndSetMapper;

public abstract class MCROAIUtils {

    public static String getDefaultRestriction(String configPrefix) {
        return MCRConfiguration2.getString(configPrefix + "Search.Restriction").orElse(null);
    }

    public static String getDefaultSetQuery(String setSpec, String configPrefix) {
        if (setSpec.contains(":")) {
            String categID = setSpec.substring(setSpec.lastIndexOf(':') + 1).trim();
            String classID = setSpec.substring(0, setSpec.indexOf(':')).trim();
            classID = MCRClassificationAndSetMapper.mapSetToClassification(configPrefix, classID);
            return "category:" + classID + "\\:" + categID;
        } else {
            String id = setSpec;
            String query = MCRConfiguration2.getString(configPrefix + "MapSetToQuery." + id.replace(":", "_"))
                .orElse("");
            if (!query.equals("")) {
                return query;
            } else {
                id = MCRClassificationAndSetMapper.mapSetToClassification(configPrefix, id);
                return "category:*" + id + "*";
            }
        }
    }

}
