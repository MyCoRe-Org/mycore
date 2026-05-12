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
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRCache;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.common.MCRXMLMetadataManager;

/**
 * Returns a classification in a configurable output format.
 * <p>
 * Syntax:
 * <pre>
 * classification:{editor[Complete]['['formatAlias']']|metadata}:{levels}
 *               [:noEmptyLeaves]:{parents|children}:{classID}[:categID]
 * </pre>
 *
 * Example:
 * <pre>
 * classification:editorComplete[mods]:2:children:myClass:root
 * </pre>
 *
 * The optional {@code formatAlias} references a configuration property:
 * <pre>
 * MCR.URIResolver.Classification.Format.&lt;formatAlias&gt;
 * </pre>
 */
public class MCRClassificationResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Pattern EDITORFORMAT_PATTERN = Pattern.compile("(\\[)([^\\]]*)(\\])");

    private static final String FORMAT_CONFIG_PREFIX = MCRURIResolver.CONFIG_PREFIX + "Classification.Format.";

    private static final String SORT_CONFIG_PREFIX = MCRURIResolver.CONFIG_PREFIX + "Classification.Sort.";

    private static MCRCache<String, Element> categoryCache;

    private static MCRCategoryDAO dao;

    static {
        try {
            dao = MCRCategoryDAO.obtainInstance();
            categoryCache = new MCRCache<>(
                MCRConfiguration2.getInt(MCRURIResolver.CONFIG_PREFIX + "Classification.CacheSize").orElse(1000),
                "URIResolver categories");
        } catch (Exception exc) {
            LOGGER.error("Unable to initialize classification resolver", exc);
        }
    }

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

    private static String getLabelFormat(String editorString) {
        Matcher m = EDITORFORMAT_PATTERN.matcher(editorString);
        if (m.find() && m.groupCount() == 3) {
            String formatDef = m.group(2);
            return MCRConfiguration2.getStringOrThrow(FORMAT_CONFIG_PREFIX + formatDef);
        }
        return null;
    }

    private static boolean shouldSortCategories(String classId) {
        return MCRConfiguration2.getBoolean(SORT_CONFIG_PREFIX + classId).orElse(true);
    }

    private static long getSystemLastModified() {
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

    private static MCRCategory getMcrCategory(String uri, String axis, String categ, String classID, int levels) {
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

    private static Element getElement(String uri, String format, String classID, MCRCategory cl,
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

}
