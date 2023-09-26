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

package org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil;

import static org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.MCRNeo4JConstants.NEO4J_CLASSID_CATEGID_SEPARATOR;
import static org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.MCRNeo4JConstants.NEO4J_CONFIG_PREFIX;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jparser.MCRNeo4JAbstractDataModelParser;

/**
 * Util for Neo4J
 *
 * @author Andreas Kluge
 * @author Jens Kupferschmidt
 */
public class MCRNeo4JUtil {

    /**
     * Retrieves the label of a classification based on the provided class ID and category ID.
     *
     * @param classidString the class ID of the classification
     * @param categidString the category ID of the classification
     * @param language      the xml:language attribute (extended by x-)
     * @return the label of the classification
     */
    public static String getClassificationLabel(String classidString, String categidString, String language) {
        String label = "";
        MCRCategoryID categid = new MCRCategoryID(classidString, categidString);
        MCRCategoryDAO dao = MCRCategoryDAOFactory.getInstance();
        MCRCategory categ = dao.getCategory(categid, 1);
        MCRLabel categLabel = categ.getLabel(language).orElse(null);
        if (categLabel != null) {
            label = categLabel.getText().replace("'", "");
        }
        return label;
    }

    /**
     * Retrieves the label of a classification based on the provided class ID and category ID.
     *
     * @param classID the class ID of the classification
     * @param categID the category ID of the classification
     * @param lang    the xml:language attribute
     * @return the label of the classification
     */
    public static Optional<String> getClassLabel(final String classID, final String categID, final String lang) {
        final MCRCategory category = MCRCategoryDAOFactory.getInstance()
                .getCategory(new MCRCategoryID(classID, categID), 1);

        if (null == category) {
            // LOGGER.warn("Category {}:{} not found!", classID, categID);
            return Optional.empty();
        }

        return category
                .getLabel(lang)
                .map(label -> StringUtils.replace(label.getText(), "'", ""));
    }

    public static String removeIllegalRelationshipTypeCharacters(String linkType) {
        String filteredType = StringUtils.replace(linkType, ":", NEO4J_CLASSID_CATEGID_SEPARATOR);
        filteredType = StringUtils.deleteWhitespace(filteredType);
        filteredType = StringUtils.remove(filteredType, '-');
        filteredType = StringUtils.remove(filteredType, '"');
        filteredType = StringUtils.remove(filteredType, '\'');
        filteredType = StringUtils.remove(filteredType, '.');
        filteredType = StringUtils.remove(filteredType, ';');

        return filteredType;
    }

    private MCRNeo4JUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Instantiates Neo4JParserClasses from properties and saves them in a Map.
     *
     * @param propertiesMap  MAP<string,string> with MCR class keys and class paths to be instantiated
     * @param filterClassKey don't instantiate a class by a given configuration key, to avoid loop instantiation
     * @return Map with parsers for each defined MCR metadata type
     */
    public static Map<String, MCRNeo4JAbstractDataModelParser> getMCRNeo4JInstantiatedParserMap(
            Map<String, String> propertiesMap, String filterClassKey) {
        final Map<String, MCRNeo4JAbstractDataModelParser> parserMap;
        parserMap = new HashMap<>();
        propertiesMap.forEach((k, v) -> {
            // avoid loop instantiation
            if (!Objects.equals(k, filterClassKey)) {
                parserMap.put(k, MCRConfiguration2.getOrThrow(NEO4J_CONFIG_PREFIX + "ParserClass." + k,
                        MCRConfiguration2::instantiateClass));
            }
        });
        return parserMap;
    }
}
