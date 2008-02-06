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
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRMetaISO8601Date;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRResults;

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
        SimpleDateFormat df = new SimpleDateFormat(simpleFormat, locale);
        MCRMetaISO8601Date mcrdate = new MCRMetaISO8601Date();
        mcrdate.setFormat(isoFormat);
        mcrdate.setDate(isoDate);
        Date date = mcrdate.getDate();
        return (date == null) ? "?"+isoDate+"?" : df.format(date);
    }

    public static String getISODate(String simpleDate, String simpleFormat, String isoFormat) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat(simpleFormat);
        df.setTimeZone(TimeZone.getTimeZone("UTC")); // or else testcase
                                                        // "1964-02-24" would
                                                        // result "1964-02-23"
        Date date = df.parse(simpleDate);
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
        Element queryElement = new Element("query");
        queryElement.setAttribute("maxResults", "0");
        queryElement.setAttribute("numPerPage", "0");
        Document input = new Document(queryElement);
        Element conditions = new Element("conditions");
        queryElement.addContent(conditions);
        conditions.setAttribute("format", "text");
        conditions.addContent(query);
        if (LOGGER.isDebugEnabled()) {
            XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
            LOGGER.debug(out.outputString(input));
        }
        long start = System.currentTimeMillis();
        MCRResults result = MCRQueryManager.search(MCRQuery.parseXML(input));
        long qtime = System.currentTimeMillis() - start;
        LOGGER.debug("total query time: " + qtime);
        return result.getNumHits();
    }
}
