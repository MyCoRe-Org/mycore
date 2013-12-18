/*
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */
package org.mycore.oai;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.oai.classmapping.MCRClassificationAndSetMapper;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryParser;
import org.mycore.services.fieldquery.MCRSortBy;

public abstract class MCROAIUtils {

    public static MCRCondition getDefaultRestriction(String configPrefix) {
        MCRConfiguration config = MCRConfiguration.instance();
        String r = config.getString(configPrefix + "Search.Restriction", null);
        if (r != null) {
            try {
                return new MCRQueryParser().parse(r);
            } catch (Exception ex) {
                String msg = "Unable to parse " + configPrefix + "Search.Restriction=" + r;
                throw new MCRConfigurationException(msg, ex);
            }
        }
        return null;
    }

    public static MCRCondition getDefaultSetCondition(String setSpec, String configPrefix) {
        if (setSpec.contains(":")) {
            String categID = setSpec.substring(setSpec.lastIndexOf(':') + 1).trim();
            String classID = setSpec.substring(0, setSpec.indexOf(':')).trim();
            classID = MCRClassificationAndSetMapper.mapSetToClassification(configPrefix, classID);
            String id = classID + ":" + categID;
            return new MCRQueryCondition("category", "=", id);
        } else {
            String id = setSpec;
            String query = MCRConfiguration.instance().getString(configPrefix + "MapSetToQuery." + id, "");
            if (!query.equals("")) {
                return new MCRQueryParser().parse(query);
            } else {
                id = MCRClassificationAndSetMapper.mapSetToClassification(configPrefix, id);
                return new MCRQueryCondition("category", "like", id + "*");
            }
        }
    }

    public static List<MCRSortBy> getSortByList(String configField, String defaultValue) {
        MCRConfiguration config = MCRConfiguration.instance();
        List<MCRSortBy> sortBy = new ArrayList<MCRSortBy>();
        String searchSortBy = config.getString(configField, defaultValue);
        for (StringTokenizer st = new StringTokenizer(searchSortBy, ",;:"); st.hasMoreTokens();) {
            String token = st.nextToken().trim();
            MCRFieldDef field = MCRFieldDef.getDef(token.split(" ")[0]);
            boolean order = "ascending".equalsIgnoreCase(token.split(" ")[1]);
            sortBy.add(new MCRSortBy(field, order));
        }
        return sortBy;
    }

}
