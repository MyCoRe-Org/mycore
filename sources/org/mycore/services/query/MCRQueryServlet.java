/*
 * $RCSfile$
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

package org.mycore.services.query;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRLayoutServlet;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.common.xml.MCRXMLSortInterface;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet provides a web interface to query the datastore using XQueries
 * and deliver the result list
 * 
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @author Mathias Hegner
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRQueryServlet extends MCRServlet {
    // TODO: we should invent something here!!
    private static final long serialVersionUID = 1L;

    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    private static final MCRQueryCollector QUERY_COLLECTOR = new MCRQueryCollector(CONFIG.getInt("MCR.Collector_Thread_num", 2), CONFIG.getInt(
            "MCR.Agent_Thread_num", 6));;

    private static final String MCR_SORTER_CONFIG_PREFIX = "MCR.XMLSorter";

    private static final String MCR_SORTER_CONFIG_DELIMITER = "\"+lang+\"";

    private static final String MCR_STANDARD_SORTER = "org.mycore.common.xml.MCRXMLSorter";

    private static final String PARAM_SORT = "SortKey";

    private static final String PARAM_IN_ORDER = "inOrder";

    private static Logger LOGGER = Logger.getLogger(MCRQueryServlet.class);

    /**
     * The initialization method for this servlet. This read the default
     * language from the configuration.
     */
    public void init() throws MCRConfigurationException, ServletException {
        super.init();
    }

    /**
     * This method handles HTTP GET/POST requests and resolves them to output.
     * 
     * @param job
     *            MCRServletJob containing request and response objects
     * @exception IOException
     *                for java I/O errors.
     * @exception ServletException
     *                for errors from the servlet engine.
     */
    public void doGetPost(MCRServletJob job) throws IOException, ServletException {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();

        HttpSession session = request.getSession(false); // if

        // session
        // exists;
        org.jdom.Document jdom = null;
        Query qParam = new Query();
        qParam.cachedFlag = false;

        // check Parameter to meet requirements
        if (!checkInputParameter(request, qParam)) {
            StringBuffer buf = new StringBuffer();
            Set entries = request.getParameterMap().entrySet();
            Iterator it = entries.iterator();
            Map.Entry entry;

            while (it.hasNext()) {
                entry = (Map.Entry) it.next();
                buf.append(entry.getKey()).append(" : ").append(MCRUtils.arrayToString((Object[]) entry.getValue(), ", ")).append("\n");
            }

            generateErrorPage(request, response, HttpServletResponse.SC_NOT_ACCEPTABLE, "Some input parameters don't meet the requirements!:\n"
                    + buf.toString(), new MCRException("Input parameter mismatch!"), false);

            return;
        }

        // Check if session is valid to performing caching functions
        if (!validateCacheSession(request, qParam)) {
            MCRException ex = new MCRException("Session invalid!");
            String sId = session.getId();
            StringBuffer msg = new StringBuffer("Requested session is invalid, maybe it was timed out!\n");
            msg.append("requested session was: ").append(request.getRequestedSessionId()).append("!\n").append("actual session is: ").append(sId).append("!");
            generateErrorPage(request, response, HttpServletResponse.SC_REQUEST_TIMEOUT, msg.toString(), ex, false);

            return;
        }

        qParam.lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        LOGGER.info("MCRQueryServlet : lang = " + qParam.lang);

        // prepare the stylesheet name
        Properties parameters = MCRLayoutServlet.buildXSLParameters(request);
        String style = parameters.getProperty("Style", qParam.mode + "-" + qParam.layout + "-" + qParam.lang);
        LOGGER.info("Style = " + style);

        // set staus for neigbours
        int status = getStatus(request);

        if (qParam.type.equals("class")) {
            jdom = queryClassification(qParam.host, qParam.type, qParam.query);

            if (jdom == null) {
                generateErrorPage(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error!", new MCRException(
                        "No classification or category exists"), false);

                return;
            }

            try {
                request.setAttribute(MCRLayoutServlet.JDOM_ATTR, jdom);
                request.setAttribute("XSL.Style", style);

                RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
                rd.forward(request, response);
            } catch (Exception ex) {
                generateErrorPage(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while forwarding XML document to LayoutServlet!", ex,
                        false);

                return;
            }

            return;
        }

        if (qParam.cachedFlag) {
            // retrieve result list from session cache
            try {
                // session at this point is valid, load objects
                jdom = (org.jdom.Document) session.getAttribute("CachedList");
                qParam.type = (String) session.getAttribute("CachedType");

                if ((jdom == null) || (qParam.type == null)) {
                    throw new MCRException("Either jdom or type (or both) were null!");
                }
            } catch (Exception ex) {
                generateErrorPage(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to get jdom and type out of session cache!", ex,
                        false);

                return;
            }

            if (qParam.customSort) {
                try {
                    // when I'm in here a ResultList exists and I have to resort
                    // it.
                    jdom = reSort(jdom, qParam);
                } catch (JDOMException e) {
                    generateErrorPage(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while RE-sorting JDOM", new MCRException(
                            "Import of elements failed due to some reason!", e), false);
                }
            } else if ((qParam.view.equals("prev") || qParam.view.equals("next")) && (qParam.referer != null)) {
                // user want's to browse the documents here..
                browse(request, response, jdom, qParam);

                return;
            } else if ((qParam.offset == 0) && (qParam.size == 0)) {
                try {
                    qParam.offset = Integer.parseInt(jdom.getRootElement().getAttributeValue("offset"));
                    qParam.size = Integer.parseInt(jdom.getRootElement().getAttributeValue("size"));
                    LOGGER.debug("Found info about last position in resultlist!");
                } catch (Exception e) {
                    LOGGER.warn("Failing to determine preset values of resultlist size and offset!");
                    qParam.offset = 0;
                    qParam.size = 0;
                }
            }
        } else {
            // cachedFlag==false
            MCRXMLContainer resarray = new MCRXMLContainer();
            putResult(QUERY_COLLECTOR, qParam.host, qParam.type, qParam.query, resarray);

            // set neighbour status for documents
            if (resarray.size() == 1) {
                resarray.setStatus(0, status);
            }

            HashSet sortTypes = new HashSet();

            // initialSort is set to true by the SearchMaskServlet
            if (qParam.mode.equals("ResultList") && qParam.saveResults && !qParam.customSort) {
                // Only in mode ResultList sorting makes sense
                StringTokenizer st = new StringTokenizer(CONFIG.getString(MCR_SORTER_CONFIG_PREFIX + ".types"), ",");

                while (st.hasMoreTokens()) {
                    sortTypes.add(st.nextToken().trim());
                }
            }

            if (qParam.customSort) {
                // when I'm in here a ResultList exists and I have to resort it.
                sort(resarray, qParam);
            } else if (sortTypes.contains(qParam.type)) {
                sort(resarray, qParam);
            }
            // cut results if more than "maxresults"
            if (qParam.maxresults > 0) {
                resarray.cutDownTo(qParam.maxresults);
            }
            jdom = resarray.exportAllToDocument();
        }

        if (qParam.mode.equals("ResultList") && qParam.saveResults) {
            jdom.getRootElement().setAttribute("offset", "" + qParam.offset).setAttribute("size", "" + qParam.size);
            session.setAttribute("CachedList", jdom);
            session.setAttribute("CachedType", qParam.type);
        }

        try {
            if (qParam.mode.equals("ResultList") && !style.equals("xml")) {
                request.setAttribute(MCRLayoutServlet.JDOM_ATTR, cutJDOM(jdom, qParam.offset, qParam.size));
            } else {
                request.setAttribute(MCRLayoutServlet.JDOM_ATTR, jdom);
            }

            request.setAttribute("XSL.Style", style);

            RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
            LOGGER.info("MCRQueryServlet: forward to MCRLayoutServlet!");
            rd.forward(request, response);
        } catch (Exception ex) {
            LOGGER.warn("Error while forwading result to MCRLayoutServlet.", ex);

            return;
        }
    }

    /**
     * <em>getBrowseElementID</em> retrieves the previous or next element ID
     * in the ResultList and gives it combined with the host back as a String in
     * the following form: <br/>status@id@host
     * 
     * @author Thomas Scheffler
     * @param jdom
     *            cached ResultList
     * @param ref
     *            the refering Document id@host
     * @param next
     *            true for next, false for previous Document
     * @return String String in the given form, representing the searched
     *         Document.
     */
    private final String getBrowseElementID(org.jdom.Document jdom, String ref, boolean next) throws MCRException, IOException {
        org.jdom.Document tempDoc = (org.jdom.Document) jdom.clone();
        LOGGER.info("MCRQueryServlet: getBrowseElementID() got: " + ref);

        StringTokenizer refGet = new StringTokenizer(ref, "@");

        if (refGet.countTokens() < 2) {
            throw new MCRException("MCRQueryServlet: Sorry \"ref\" has not 2 Tokens: " + ref);
        }

        String id = refGet.nextToken();
        String host = refGet.nextToken();
        List elements = tempDoc.getRootElement().getChildren(MCRXMLContainer.TAG_RESULT);
        org.jdom.Element search = null;
        org.jdom.Element prev = null;

        while (!elements.isEmpty()) {
            search = (Element) elements.get(0);

            if (search.getAttributeValue("id").equals(id) && search.getAttributeValue("host").equals(host)) {
                if (next) {
                    search = (Element) elements.get(1);
                    elements.clear();
                } else {
                    search = prev;
                    elements.clear();
                }
            } else {
                prev = search;
                elements.remove(0);
            }
        }

        if (search == null) {
            throw new MCRException("MCRQueryServlet: Sorry doesn't found searched document");
        }

        int status = ((search.getAttributeValue(MCRXMLContainer.ATTR_SUCC).equals("true")) ? 1 : 0)
                + ((search.getAttributeValue(MCRXMLContainer.ATTR_PRED).equals("true")) ? 2 : 0);
        id = search.getAttributeValue("id");
        host = search.getAttributeValue("host");

        String result = new StringBuffer().append(status).append('@').append(id).append('@').append(host).toString();
        LOGGER.info("MCRQueryServlet: getBrowseElementID() returns: " + result);

        return result;
    }

    private final MCRXMLContainer sort(MCRXMLContainer xmlcont, Query qParam) {
        MCRXMLSortInterface sorter = null;

        try {
            sorter = (MCRXMLSortInterface) (Class.forName(CONFIG.getString("MCR.XMLSorter." + qParam.type + ".InterfaceImpl", MCR_STANDARD_SORTER)))
                    .newInstance();
        } catch (InstantiationException e) {
            throw new MCRException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new MCRException(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new MCRException(e.getMessage(), e);
        }

        if (sorter.getServletContext() == null) {
            sorter.setServletContext(getServletContext());
        }

        if (qParam.customSort) {
            LOGGER.info("MCRQueryServlet: CustomSort enalbed. Sorting inorder: " + qParam.inOrder);
            sorter.addSortKey(replString(qParam.sortKey, MCR_SORTER_CONFIG_DELIMITER, qParam.lang), qParam.inOrder);
        } else {
            LOGGER.info("MCRQueryServlet: default sorting enabled by properties...");

            StringBuffer prop = new StringBuffer();
            prop.append(MCR_SORTER_CONFIG_PREFIX).append('.').append(qParam.type).append(".keys.count");

            int keynum = Integer.parseInt(CONFIG.getString(prop.toString(), "0"));
            boolean inorder = true;
            int prefix = MCR_SORTER_CONFIG_PREFIX.length() + 1 + qParam.type.length() + ".keys.".length();

            for (int key = 1; key <= keynum; key++) {
                // get XPATH Expression and hope it's good, if not exist sort
                // for title
                prop.delete(prefix, prop.length()).append(key).append(".inorder");
                inorder = CONFIG.getBoolean(prop.toString(), true);
                prop.delete(prefix, prop.length()).append(key);
                sorter.addSortKey(replString(CONFIG.getString(prop.toString(), "./*/*/*/title[lang('" + qParam.lang + "')]"), MCR_SORTER_CONFIG_DELIMITER, qParam.lang),
                        inorder);
            }
        }

        xmlcont.sort(sorter);

        return xmlcont;
    }

    private static String replString(String parse, String from, String to) {
        StringBuffer result = new StringBuffer(parse);

        if ((result.charAt(0) == '\"') && (result.charAt(result.length() - 1) == '\"')) {
            result.deleteCharAt(result.length() - 1).deleteCharAt(0);

            for (int i = result.toString().indexOf(from); i != -1; i = result.toString().indexOf(from)) {
                result.replace(i, i + from.length(), to);
            }

            return result.toString();
        }

        return null;
    }

    private final Document cutJDOM(Document jdom, int offset, int size) {
        LOGGER.debug("Cutting to " + size + " at offset " + offset);

        Document returns = (Document) jdom.clone();
        returns.getRootElement().removeChildren("mcr_result");

        List children = jdom.getRootElement().getChildren("mcr_result");

        if (size <= 0) {
            offset = 0;
            size = children.size();
        }

        int amount = size;

        for (int i = offset; ((amount > 0) && (i < children.size()) && (i < (offset + size))); i++) {
            returns.getRootElement().addContent((Element) ((Element) children.get(i)).clone());
            amount--;
        }

        returns.getRootElement().setAttribute("count", "" + children.size()).setAttribute("offset", "" + offset).setAttribute("size", "" + size);

        return returns;
    }

    private static final boolean isInstanceOfLocal(String host, String servletHost, String servletPath, int servletPort) {
        final String confPrefix = "MCR.remoteaccess_";
        String RemoteHost = CONFIG.getString(confPrefix + host + "_host");
        String queryServletPath = servletPath.substring(0, servletPath.lastIndexOf("/")) + "/MCRQueryServlet";
        String remotePath = CONFIG.getString(confPrefix + host + "_query_servlet");
        int remotePort = Integer.parseInt(CONFIG.getString(confPrefix + host + "_port"));

        return ((RemoteHost.equals(servletHost)) && (remotePath.equals(queryServletPath)) && (servletPort == remotePort)) ? true : false;
    }

    private final boolean checkInputParameter(HttpServletRequest request, Query qParam) {
        qParam.mode = getProperty(request, "mode");
        qParam.query = getProperty(request, "query");
        qParam.type = getProperty(request, "type");
        qParam.layout = getProperty(request, "layout");

        String saveResults_str = getProperty(request, "saveResults");

        if ((saveResults_str != null) && saveResults_str.equals("true")) {
            qParam.saveResults = true;
        } else {
            qParam.saveResults = false;
        }

        // multiple host are allowed
        qParam.hosts = request.getParameterValues("hosts");
        qParam.att_host = (String) request.getAttribute("hosts");

        // dont't overwrite host if getParameter("hosts") was successful
        qParam.host = "";

        if ((qParam.att_host != null) && ((qParam.hosts == null) || (qParam.hosts.length == 0))) {
            qParam.host = qParam.att_host;
        } else if ((qParam.hosts != null) && (qParam.hosts.length > 0)) {
            // find a Instance of the local one
            String ServerName = request.getServerName();
            LOGGER.info("MCRQueryServlet: Try to map remote request to local one!");
            LOGGER.info("MCRQueryServlet: Local Server Name=" + ServerName);

            StringBuffer hostBf = new StringBuffer();

            for (int i = 0; i < qParam.hosts.length; i++) {
                if (!qParam.hosts[i].equals("local")) {
                    // the following replaces a remote request with "local" if
                    // needed
                    qParam.hosts[i] = (isInstanceOfLocal(qParam.hosts[i], request.getServerName(), request.getServletPath(), request.getServerPort())) ? "local"
                            : qParam.hosts[i];
                }

                // make a comma seperated list of all hosts
                hostBf.append(",").append(qParam.hosts[i]);
            }

            qParam.host = hostBf.deleteCharAt(0).toString();

            if (qParam.host.indexOf("local") != qParam.host.lastIndexOf("local")) {
                LOGGER.info("MCRQueryServlet: multiple \"local\" will be removed by MCRQueryResult!");
            }
        }

        qParam.view = request.getParameter("view");
        qParam.referer = request.getParameter("ref");

        String offsetStr = request.getParameter("offset");
        String sizeStr = request.getParameter("size");
        String max_results = request.getParameter("max_results");
        qParam.sortKey = request.getParameter(PARAM_SORT);

        if (qParam.sortKey != null) {
            if ((request.getParameter(PARAM_IN_ORDER) != null) && request.getParameter(PARAM_IN_ORDER).toLowerCase().equals("false")) {
                qParam.inOrder = false;
            } else {
                qParam.inOrder = true;
            }

            qParam.customSort = true;

            // if customSort enabled we must saveResults too
            qParam.saveResults = true;
        } else {
            qParam.customSort = false;
        }

        if (max_results != null) {
            qParam.maxresults = Integer.parseInt(max_results);
        }

        qParam.offset = 0;

        if (offsetStr != null) {
            qParam.offset = Integer.parseInt(offsetStr);
        }

        qParam.size = 0;

        if (sizeStr != null) {
            qParam.size = Integer.parseInt(sizeStr);
        }

        if (qParam.mode == null) {
            qParam.mode = "ResultList";
        }

        if (qParam.mode.equals("")) {
            qParam.mode = "ResultList";
        }

        if (qParam.host == null) {
            qParam.host = "local";
        }

        if (qParam.host.equals("")) {
            qParam.host = "local";
        }

        if (qParam.query == null) {
            qParam.query = "";
        }

        if (qParam.type == null) {
            LOGGER.debug("Parameter type is NULL!");

            return false;
        }

        if (qParam.type.equals("")) {
            LOGGER.debug("Parameter type is EMPTY!");

            return false;
        }

        if (qParam.layout == null) {
            qParam.layout = qParam.type;
        }

        if (qParam.layout.equals("")) {
            qParam.layout = qParam.type;
        }

        qParam.type = qParam.type.toLowerCase();

        if (qParam.view == null) {
            qParam.view = "";
        } else {
            qParam.view = qParam.view.toLowerCase();
        }

        LOGGER.info("MCRQueryServlet: RequestEncoding = " + request.getCharacterEncoding());
        LOGGER.info("MCRQueryServlet: ContentType = " + request.getContentType());
        LOGGER.info("MCRQueryServlet : mode = " + qParam.mode);
        LOGGER.info("MCRQueryServlet : type = " + qParam.type);
        LOGGER.info("MCRQueryServlet : layout = " + qParam.layout);
        LOGGER.info("MCRQueryServlet : hosts = " + qParam.host);
        LOGGER.info("MCRQueryServlet : query = \"" + qParam.query + "\"");

        return true;
    }

    private final boolean validateCacheSession(HttpServletRequest request, Query qParam) {
        // check for valid session
        if (qParam.mode.equals("CachedResultList")) {
            if (!request.isRequestedSessionIdValid()) {
                // page session timed out
                return false;
            }

            qParam.cachedFlag = true;
            qParam.mode = "ResultList";
        }

        return true;
    }

    private static final void putResult(MCRQueryCollector collector, String host, String type, String query, MCRXMLContainer result) {
        if (host.indexOf(',') < 0) {
            /*
             * we are querying only a single host- don't bother to put
             * everything through the two thread pools, but just return the
             * result
             */
            LOGGER.debug("Retrieving query " + query + " from MCRQueryCache (hostlist: " + host + ") type=" + type + " maxresults="
                    + CONFIG.getInt("MCR.query_max_results", 10));
            result.importElements(MCRQueryCache.getResultList(host, query, type, CONFIG.getInt("MCR.query_max_results", 10)));
        } else {
            try {
                synchronized (result) {
                    collector.collectQueryResults(host, type, query, result);
                    result.wait();
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    private final int getStatus(HttpServletRequest request) {
        int status = (getProperty(request, "status") != null) ? Integer.parseInt(getProperty(request, "status")) : 0;

        if (LOGGER.isDebugEnabled()) {
            boolean successor = ((status % 2) == 1) ? true : false;
            boolean predecessor = (((status >> 1) % 2) == 1) ? true : false;
            LOGGER.debug("MCRQueryServlet : status = " + status);
            LOGGER.debug("MCRQueryServlet : predecessor = " + predecessor);
            LOGGER.debug("MCRQueryServlet : successor = " + successor);
        }

        return status;
    }

    private final Document queryClassification(String host, String type, String query) {
        String squence = CONFIG.getString("MCR.classifications_search_sequence", "remote-local");
        MCRXMLContainer resarray = new MCRXMLContainer();

        if (squence.equalsIgnoreCase("local-remote")) {
            putResult(QUERY_COLLECTOR, "local", type, query, resarray);

            if (resarray.size() == 0) {
                putResult(QUERY_COLLECTOR, host, type, query, resarray);
            }
        } else {
            putResult(QUERY_COLLECTOR, host, type, query, resarray);

            if (resarray.size() == 0) {
                putResult(QUERY_COLLECTOR, "local", type, query, resarray);
            }
        }

        if (resarray.size() == 0) {
            return null;
        }

        return resarray.exportAllToDocument();
    }

    private final Document reSort(Document jdom, Query qParam) throws ServletException, IOException, MCRException, JDOMException {
        MCRXMLContainer resarray = new MCRXMLContainer();
        resarray.importElements(jdom);

        if (resarray.size() > 0) {
            // let's do resorting.
            return sort(resarray, qParam).exportAllToDocument();
        }

        LOGGER.fatal("MCRQueryServlet: Error while RE-sorting JDOM:" + "After import Containersize was ZERO!");

        return jdom;
    }

    private final void browse(HttpServletRequest request, HttpServletResponse response, Document jdom, Query qParam) throws ServletException, IOException {
        /* change generate new query */
        StringTokenizer refGet = null;

        try {
            refGet = new StringTokenizer(this.getBrowseElementID(jdom, qParam.referer, qParam.view.equals("next")), "@");
        } catch (Exception ex) {
            generateErrorPage(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not resolve browse origin!", ex, false);

            return;
        }

        if (refGet.countTokens() < 3) {
            generateErrorPage(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not resolve browse origin!", new MCRException(
                    "MCRQueryServlet: Sorry \"refGet\" has not 3 Tokens: " + refGet), false);

            return;
        }

        String StrStatus = refGet.nextToken();
        qParam.query = new StringBuffer("/mycoreobject[@ID='").append(refGet.nextToken()).append("']").toString();
        qParam.host = refGet.nextToken();
        qParam.mode = "ObjectMetadata";
        request.setAttribute("mode", qParam.mode);
        request.removeAttribute("status");
        request.setAttribute("status", StrStatus);
        request.setAttribute("type", qParam.type);
        request.setAttribute("layout", qParam.layout);
        request.setAttribute("hosts", qParam.host);
        request.setAttribute("lang", qParam.lang);
        request.setAttribute("query", qParam.query);
        request.setAttribute("view", "done");
        LOGGER.info(new StringBuffer("MCRQueryServlet: sending to myself:").append("?mode=").append(qParam.mode).append("&status=").append(StrStatus).append(
                "&type=").append(qParam.type).append("&hosts=").append(qParam.host).append("&lang=").append(qParam.lang).append("&query=").append(qParam.query)
                .toString());
        doGet(request, response);

        return;
    }

    private static final class Query {
        private boolean customSort = false;

        private String sortKey;

        private boolean inOrder = true;

        private boolean saveResults;

        private String mode;

        private String query;

        private String type;

        private String layout;

        private String lang;

        private String[] hosts;

        private String att_host;

        private String view;

        private String referer;

        private String host;

        private int maxresults = 0;

        private int offset;

        private int size;

        private boolean cachedFlag;
    }
}
