/*
 * 
 * $Revision$ $Date$
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

package org.mycore.common.xml;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRURN;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRMetaISO8601Date;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRQueryParser;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.urn.MCRURNManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Thomas Scheffler (yagee)
 * 
 */
public class MCRXMLFunctions {

    static MCRConfiguration CONFIG = MCRConfiguration.instance();

    private static final String HOST_PREFIX = "MCR.remoteaccess_";

    private static final String QUERY_SUFFIX = "_query_servlet";

    private static final String IFS_SUFFIX = "_ifs_servlet";

    private static final String HOST_SUFFIX = "_host";

    private static final String PORT_SUFFIX = "_port";

    private static final String PROTOCOLL_SUFFIX = "_protocol";

    private static final String DEFAULT_PORT = "80";

    private static final Logger LOGGER = Logger.getLogger(MCRXMLFunctions.class);

    private static final DocumentBuilder DOC_BUILDER;
    static {
        DocumentBuilder documentBuilder = null;
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            LOGGER.error("Could not instantiate DocumentBuilder. Not all functions will be available.", e);
        }
        DOC_BUILDER = documentBuilder;
    }

    /**
     * returns the given String trimmed
     * 
     * @param arg0
     *            String to be trimmed
     * @return trimmed copy of arg0
     * @see java.lang.String#trim()
     */
    public static String trim(String arg0) {
        return arg0.trim();
    }

    /**
     * returns the QueryServlet-Link of the given hostAlias
     * 
     * @param hostAlias
     *            remote alias
     * @return QueryServlet-Link
     */
    public static String getQueryServlet(String hostAlias) {
        return getBaseLink(hostAlias).append(CONFIG.getString(new StringBuffer(HOST_PREFIX).append(hostAlias).append(QUERY_SUFFIX).toString())).toString();
    }

    /**
     * returns the FileNodeServlet-Link of the given hostAlias
     * 
     * @param hostAlias
     *            remote alias
     * @return FileNodeServlet-Link
     */
    public static String getIFSServlet(String hostAlias) {
        return getBaseLink(hostAlias).append(CONFIG.getString(new StringBuffer(HOST_PREFIX).append(hostAlias).append(IFS_SUFFIX).toString())).toString();
    }

    public static StringBuffer getBaseLink(String hostAlias) {
        StringBuffer returns = new StringBuffer();
        returns.append(CONFIG.getString(new StringBuffer(HOST_PREFIX).append(hostAlias).append(PROTOCOLL_SUFFIX).toString(), "http")).append("://").append(
            CONFIG.getString(new StringBuffer(HOST_PREFIX).append(hostAlias).append(HOST_SUFFIX).toString()));
        String port = CONFIG.getString(new StringBuffer(HOST_PREFIX).append(hostAlias).append(PORT_SUFFIX).toString(), DEFAULT_PORT);
        if (!port.equals(DEFAULT_PORT)) {
            returns.append(":").append(port);
        }
        return returns;
    }

    public static String formatISODate(String isoDate, String simpleFormat, String iso639Language) throws ParseException {
        return formatISODate(isoDate, null, simpleFormat, iso639Language);
    }

    public static String formatISODate(String isoDate, String isoFormat, String simpleFormat, String iso639Language) throws ParseException {
        if (LOGGER.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer("isoDate=");
            sb.append(isoDate).append(", simpleFormat=").append(simpleFormat).append(", isoFormat=").append(isoFormat).append(", iso649Language=").append(
                iso639Language);
            LOGGER.debug(sb.toString());
        }
        Locale locale = new Locale(iso639Language);
        MCRMetaISO8601Date mcrdate = new MCRMetaISO8601Date();
        mcrdate.setFormat(isoFormat);
        mcrdate.setDate(isoDate);
        String formatted = mcrdate.format(simpleFormat, locale);
        return (formatted == null) ? "?" + isoDate + "?" : formatted;
    }

    public static String getISODate(String simpleDate, String simpleFormat, String isoFormat) throws ParseException {
        Date date;
        if (simpleFormat.equals("long")) {
            date = new Date(Long.parseLong(simpleDate));
        } else {
            SimpleDateFormat df = new SimpleDateFormat(simpleFormat);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            // or else testcase
            // "1964-02-24" would
            // result "1964-02-23"
            date = df.parse(simpleDate);
        }
        MCRMetaISO8601Date mcrdate = new MCRMetaISO8601Date();
        mcrdate.setDate(date);
        mcrdate.setFormat(isoFormat);
        return mcrdate.getISOString();
    }

    public static String getISODate(String simpleDate, String simpleFormat) throws ParseException {
        return getISODate(simpleDate, simpleFormat, null);
    }

    public static String regexp(String orig, String match, String replace) {
        return orig.replaceAll(match, replace);
    }

    public static int getQueryHitCount(String query) {
        MCRCondition condition = new MCRQueryParser().parse(query);
        MCRQuery q = new MCRQuery(condition);
        long start = System.currentTimeMillis();
        MCRResults result = MCRQueryManager.search(q);
        if (LOGGER.isDebugEnabled()) {
            long qtime = System.currentTimeMillis() - start;
            LOGGER.debug("total query time: " + qtime);
        }
        return result.getNumHits();
    }

    /**
     * @return true if the given object has an urn assigned, false otherwise
     * */
    public static boolean hasURNDefined(String objId) {
        if (objId == null) {
            return false;
        }
        try {
            return MCRURNManager.hasURNAssigned(objId);
        } catch (Exception ex) {
            LOGGER.error("Error while retrieving urn from database for object " + objId, ex);
            return false;
        }
    }

    /**
     * returns the URN for <code>mcrid</code> and children if <code>mcrid</code> is a derivate.
     * @param mcrid MCRObjectID of object or derivate
     * @return list of mcrid|file to urn mappings
     */
    public static NodeList getURNsForMCRID(String mcrid) {
        Session session = MCRHIBConnection.instance().getSession();
        Criteria criteria = session.createCriteria(MCRURN.class);
        criteria.add(Restrictions.eq("key.mcrid", mcrid));
        Document document = DOC_BUILDER.newDocument();
        Element rootElement = document.createElement("urn");
        document.appendChild(rootElement);
        @SuppressWarnings("unchecked")
        List<MCRURN> results = criteria.list();
        for (MCRURN result : results) {
            String path = result.getPath().trim();
            if (path.length() > 0 && path.charAt(0) == '/') {
                path = path.substring(1);
            }
            path += result.getFilename().trim();
            if (path.length() > 0) {
                Element file = document.createElement("file");
                file.setAttribute("urn", result.getKey().getMcrurn());
                file.setAttribute("name", path);
                rootElement.appendChild(file);
            } else {
                rootElement.setAttribute("mcrid", result.getKey().getMcrid());
                rootElement.setAttribute("urn", result.getKey().getMcrurn());
            }
            session.evict(result);
        }
        return rootElement.getChildNodes();
    }

    public static boolean classAvailable(String className) {
        try {
            Class.forName(className);
            LOGGER.debug("found class: " + className);
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.debug("did not found class: " + className);
            return false;
        }
    }
}
