/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.common.xsl.uriresolver;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRCache;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRPropertyMap;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.common.MCRXMLMetadataManager;

/**
 * {@link URIResolver} that returns a classification in a configurable output format.
 * <p>Results are cached and invalidated based on the last modification time of the
 * classification and XML metadata store.
 */
@MCRConfigurationProxy(proxyClass = MCRClassificationURIResolver.Factory.class)
public class MCRClassificationURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Pattern EDITORFORMAT_PATTERN = Pattern.compile("(\\[)([^\\]]*)(\\])");

    private final Map<String, String> formatMap;

    private final Map<String, Boolean> sortMap;

    private final MCRCache<String, Element> categoryCache;

    private final MCRCategoryDAO dao;

    /**
     * Creates a new {@code MCRClassificationURIResolver} with the given settings.
     *
     * @param cacheCapacity maximum number of entries the cache may hold
     * @param formatMap map of format alias names to label format strings
     * @param sortMap map of classification IDs to sort flags;
     *                if a classification ID is absent, categories are sorted by default
     */
    public MCRClassificationURIResolver(int cacheCapacity, Map<String, String> formatMap,
        Map<String, Boolean> sortMap) {
        categoryCache = new MCRCache<>(cacheCapacity, "URIResolver categories");
        dao = MCRCategoryDAO.obtainInstance();
        this.formatMap = formatMap;
        this.sortMap = sortMap;
    }

    /**
     * Resolves the given URI and returns the requested classification as an XML source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{editor[Complete]['['formatAlias']']|metadata}:{levels|all}
     *            [:noEmptyLeaves]:{parents|children}:{classID}[:categID]
     * </pre>
     * <p>The optional {@code formatAlias} references the configuration property
     * {@code MCR.URIResolver.Classification.Format.<formatAlias>}.
     * If {@code noEmptyLeaves} is specified, leaf categories without entries are excluded.
     * <p>Example request:
     * <pre>
     *   classification:editorComplete[mods]:2:children:myClass:root
     *   classification:metadata:all:noEmptyLeaves:parents:myClass:someCateg
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <items>
     *     <item value="root">
     *       <label xml:lang="en">Root Category</label>
     *     </item>
     *   </items>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping the classification element, or an empty source if not found
     * @throws IllegalArgumentException if the URI does not match the expected syntax or contains an unknown format
     */
    @Override
    public Source resolve(String href, String base) {
        LOGGER.debug("start resolving {}", href);
        String cacheKey = getCacheKey(href);
        Element returns = categoryCache.getIfUpToDate(cacheKey, getSystemLastModified());
        if (returns == null) {
            returns = getClassElement(href);
            if (returns != null) {
                categoryCache.put(cacheKey, returns);
            }
        }
        return new JDOMSource(returns);
    }

    protected String getCacheKey(String uri) {
        return uri;
    }

    private String getLabelFormat(String editorString) {
        Matcher m = EDITORFORMAT_PATTERN.matcher(editorString);
        if (m.find() && m.groupCount() == 3) {
            String formatDef = m.group(2);
            String result = formatMap.get(formatDef);
            if (result == null) {
                throw new MCRConfigurationException("Format " + formatDef + " is not configured");
            }
            return result;
        }
        return null;
    }

    private boolean shouldSortCategories(String classId) {
        return sortMap.getOrDefault(classId, true);
    }

    private long getSystemLastModified() {
        long xmlLastModified = MCRXMLMetadataManager.obtainInstance().getLastModified();
        long classLastModified = dao.getLastModified();
        return Math.max(xmlLastModified, classLastModified);
    }

    private Element getClassElement(String uri) {
        StringTokenizer pst = new StringTokenizer(uri, ":", true);
        if (pst.countTokens() < 9) {
            // sanity check
            throw new IllegalArgumentException("Invalid format of uri for retrieval of classification: " + uri);
        }

        pst.nextToken(); // "classification"
        pst.nextToken(); // :
        String format = pst.nextToken();
        pst.nextToken(); // :

        String levelS = pst.nextToken();
        pst.nextToken(); // :
        int levels = Objects.equals(levelS, "all") ? -1 : Integer.parseInt(levelS);

        String axis;
        String token = pst.nextToken();
        pst.nextToken(); // :
        boolean emptyLeaves = !Objects.equals(token, "noEmptyLeaves");
        if (!emptyLeaves) {
            axis = pst.nextToken();
            pst.nextToken(); // :
        } else {
            axis = token;
        }

        String classID = pst.nextToken();
        StringBuilder categID = new StringBuilder();
        if (pst.hasMoreTokens()) {
            pst.nextToken(); // :
            while (pst.hasMoreTokens()) {
                categID.append(pst.nextToken());
            }
        }

        String categ;
        try {
            categ = MCRXMLFunctions.decodeURIPath(categID.toString());
        } catch (URISyntaxException e) {
            categ = categID.toString();
        }
        MCRCategory cl = getMcrCategory(uri, axis, categ, classID, levels);
        if (cl == null) {
            return null;
        }
        return getElement(uri, format, classID, cl, emptyLeaves);
    }

    private MCRCategory getMcrCategory(String uri, String axis, String categ, String classID, int levels) {
        MCRCategory cl = null;
        LOGGER.debug("categoryCache entry invalid or not found: start MCRClassificationQuery");
        if (axis.equals("children")) {
            if (!categ.isEmpty()) {
                cl = dao.getCategory(new MCRCategoryID(classID, categ), levels);
            } else {
                cl = dao.getCategory(new MCRCategoryID(classID), levels);
            }
        } else if (axis.equals("parents")) {
            if (categ.isEmpty()) {
                LOGGER.error("Cannot resolve parent axis without a CategID. URI: {}", uri);
                throw new IllegalArgumentException(
                    "Invalid format (categID is required in mode 'parents') "
                        + "of uri for retrieval of classification: "
                        + uri);
            }
            cl = dao.getRootCategory(new MCRCategoryID(classID, categ), levels);
        }
        if (cl == null) {
            return null;
        }
        return cl;
    }

    private Element getElement(String uri, String format, String classID, MCRCategory cl,
        boolean emptyLeaves) {
        Element returns;
        LOGGER.debug("start transformation of ClassificationQuery");
        if (format.startsWith("editor")) {
            boolean completeId = format.startsWith("editorComplete");
            boolean sort = shouldSortCategories(classID);
            String labelFormat = getLabelFormat(format);
            if (labelFormat == null) {
                returns = MCRCategoryTransformer.getEditorItems(cl, sort, emptyLeaves, completeId);
            } else {
                returns = MCRCategoryTransformer.getEditorItems(cl, labelFormat, sort, emptyLeaves, completeId);
            }
        } else if (format.equals("metadata")) {
            returns = MCRCategoryTransformer.getMetaDataDocument(cl, false).getRootElement().detach();
        } else {
            LOGGER.error("Unknown target format given. URI: {}", uri);
            throw new IllegalArgumentException(
                "Invalid target format (" + format + ") in uri for retrieval of classification: " + uri);
        }
        LOGGER.debug("end resolving {}", uri);
        return returns;
    }

    /**
     * Factory that creates {@link MCRClassificationURIResolver} instances from MyCoRe configuration properties.
     */
    public static class Factory implements Supplier<MCRClassificationURIResolver> {

        /**
         * Maximum number of entries the cache may hold.
         */
        @MCRProperty(name = "CacheCapacity")
        public String capacity;

        /**
         * Map of format alias names to label format strings.
         */
        @MCRPropertyMap(name = "Format")
        public Map<String, String> formatMap;

        /**
         * Optional map of classification IDs to sort flags.
         * Classifications absent from this map are sorted by default.
         */
        @MCRPropertyMap(name = "Sort", required = false)
        public Map<String, Boolean> sortMap;

        @Override
        public MCRClassificationURIResolver get() {
            return new MCRClassificationURIResolver(Integer.parseInt(capacity), formatMap, sortMap);
        }

    }

}
