/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.services.query;

import java.io.*;
import java.util.*;

import javax.servlet.http.*;
import javax.servlet.*;

import org.jdom.*;
import org.mycore.common.*;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.common.xml.MCRLayoutServlet;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.common.xml.MCRXMLSortInterface;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * This servlet provides a web interface to query the datastore using XQueries
 * and deliver the result list
 * 
 * @author Frank L�tzenkirchen
 * @author Jens Kupferschmidt
 * @author Mathias Hegner
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRQueryServlet extends MCRServlet {
	//TODO: we should invent something here!!
	private static final long serialVersionUID = 1L;

	// The configuration
	private static MCRConfiguration MCR_CONFIG = null;

	private MCRQueryCollector collector;

	// Default Language
	private String defaultLang = "";

	private static final String MCR_SORTER_CONFIG_PREFIX = "MCR.XMLSorter";

	private static final String MCR_SORTER_CONFIG_DELIMITER = "\"+lang+\"";

	private static final String MCR_STANDARD_SORTER = "org.mycore.common.xml.MCRXMLSorter";

	private static final String PARAM_SORT = "SortKey";

	private static final String PARAM_IN_ORDER = "inOrder";

	private boolean customSort = false;

	private String sortKey;

	private boolean inOrder = true;

	private static Logger LOGGER = Logger.getLogger(MCRQueryServlet.class);

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

	/**
	 * The initialization method for this servlet. This read the default
	 * language from the configuration.
	 */
	public void init() throws MCRConfigurationException {
		super.init();
		MCR_CONFIG = super.config;
		collector = (MCRQueryCollector) this.getServletContext().getAttribute(
				"QueryCollector");
		if (collector == null)
			collector = new MCRQueryCollector(MCR_CONFIG.getInt(
					"MCR.Collector_Thread_num", 2), MCR_CONFIG.getInt(
					"MCR.Agent_Thread_num", 6));
		this.getServletContext().setAttribute("QueryCollector", collector);
		PropertyConfigurator.configure(MCR_CONFIG.getLoggingProperties());
		defaultLang = MCR_CONFIG.getString("MCR.metadata_default_lang", "de");
	}

	/**
	 * This method handles HTTP GET/POST requests and resolves them to output.
	 * 
	 * @param job
	 *                    MCRServletJob containing request and response objects
	 * @exception IOException
	 *                         for java I/O errors.
	 * @exception ServletException
	 *                         for errors from the servlet engine.
	 */
	public void doGetPost(MCRServletJob job) throws IOException,
			ServletException {
		HttpServletRequest request = job.getRequest();
		HttpServletResponse response = job.getResponse();
		cachedFlag = false;
		HttpSession session = request.getSession(false); //if
		// session
		// exists;

		org.jdom.Document jdom = null;

		//check Parameter to meet requirements
		if (!checkInputParameter(request)) {
			generateErrorPage(request, response,
					HttpServletResponse.SC_NOT_ACCEPTABLE,
					"Some input parameters don't meet the requirements!",
					new MCRException("Input parameter mismatch!"), false);
			return;
		}
		// Check if session is valid to performing caching functions
		if (!validateCacheSession(request)) {
			MCRException ex = new MCRException("Session invalid!");
			String sId = session.getId();
			StringBuffer msg = new StringBuffer(
					"Requested session is invalid, maybe it was timed out!\n");
			msg.append("requested session was: ").append(
					request.getRequestedSessionId()).append("!\n").append(
					"actual session is: ").append(sId).append("!");
			generateErrorPage(request, response,
					HttpServletResponse.SC_REQUEST_TIMEOUT, msg.toString(), ex,
					false);
			return;
		}

		// prepare the stylesheet name
		Properties parameters = MCRLayoutServlet.buildXSLParameters(request);
		String style = parameters.getProperty("Style", mode + "-" + layout
				+ "-" + lang);
		LOGGER.info("Style = " + style);

		// set staus for neigbours
		int status = getStatus(request);

		if (type.equals("class")) {
			jdom = queryClassification(host, type, query);
			if (jdom == null) {
				generateErrorPage(request, response,
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Internal Server Error!", new MCRException(
								"No classification or category exists"), false);
				return;
			}
			try {
				request.setAttribute(MCRLayoutServlet.JDOM_ATTR, jdom);
				request.setAttribute("XSL.Style", style);
				RequestDispatcher rd = getServletContext().getNamedDispatcher(
						"MCRLayoutServlet");
				rd.forward(request, response);
			} catch (Exception ex) {
				generateErrorPage(
						request,
						response,
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Error while forwarding XML document to LayoutServlet!",
						ex, false);
				return;
			}
			return;
		}

		if (cachedFlag) {
			// retrieve result list from session cache
			try {
				//session at this point is valid, load objects
				jdom = (org.jdom.Document) session.getAttribute("CachedList");
				type = (String) session.getAttribute("CachedType");
				if (jdom == null || type == null)
					throw new MCRException(
							"Either jdom or type (or both) were null!");
			} catch (Exception ex) {
				generateErrorPage(request, response,
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Failed to get jdom and type out of session cache!",
						ex, false);
				return;
			}

			if (customSort) {
				try {
					// when I'm in here a ResultList exists and I have to resort
					// it.
					jdom = reSort(jdom);
				} catch (JDOMException e) {
					generateErrorPage(
							request,
							response,
							HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							"Error while RE-sorting JDOM",
							new MCRException(
									"Import of elements failed due to some reason!",
									e), false);
				}
			} else if ((view.equals("prev") || view.equals("next"))
					&& (referer != null)) {
				// user want's to browse the documents here..
				browse(request, response, jdom);
				return;
			} else if (offset == 0 && size == 0) {
				try {
					offset = Integer.parseInt(jdom.getRootElement()
							.getAttributeValue("offset"));
					size = Integer.parseInt(jdom.getRootElement()
							.getAttributeValue("size"));
					LOGGER
							.debug("Found info about last position in resultlist!");
				} catch (Exception e) {
					LOGGER
							.warn("Failing to determine preset values of resultlist size and offset!");
					offset = 0;
					size = 0;
				}
			}
		} else {
			//cachedFlag==false
			MCRXMLContainer resarray = new MCRXMLContainer();
			putResult(collector, host, type, query, resarray);
			// set neighbour status for documents
			if (resarray.size() == 1)
				resarray.setStatus(0, status);
			// cut results if more than "maxresults"
			else if (maxresults > 0)
				resarray.cutDownTo(maxresults);
			jdom = resarray.exportAllToDocument();
			if (customSort) {
				// when I'm in here a ResultList exists and I have to resort it.
				try {
					jdom = reSort(jdom);
				} catch (JDOMException e) {
					generateErrorPage(
							request,
							response,
							HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							"Error while RE-sorting JDOM",
							new MCRException(
									"Import of elements failed due to some reason!",
									e), false);
				}
			}
		}
		if (mode.equals("ResultList")) {
			jdom.getRootElement().setAttribute("offset", "" + offset)
					.setAttribute("size", "" + size);
			session.setAttribute("CachedList", jdom);
			session.setAttribute("CachedType", type);
		}
		try {
			if (mode.equals("ResultList") && !style.equals("xml")) {
				request.setAttribute(MCRLayoutServlet.JDOM_ATTR, cutJDOM(jdom,
						offset, size));
			} else
				request.setAttribute(MCRLayoutServlet.JDOM_ATTR, jdom);
			request.setAttribute("XSL.Style", style);
			RequestDispatcher rd = getServletContext().getNamedDispatcher(
					"MCRLayoutServlet");
			LOGGER.info("MCRQueryServlet: forward to MCRLayoutServlet!");
			rd.forward(request, response);
		} catch (Exception ex) {
			generateErrorPage(request, response,
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Error while forwarding XML document to LayoutServlet!",
					ex, false);
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
	 *                    cached ResultList
	 * @param ref
	 *                    the refering Document id@host
	 * @param next
	 *                    true for next, false for previous Document
	 * @return String String in the given form, representing the searched
	 *               Document.
	 */
	private final String getBrowseElementID(org.jdom.Document jdom, String ref,
			boolean next) throws MCRException, IOException {
		org.jdom.Document tempDoc = (org.jdom.Document) jdom.clone();
		LOGGER.info("MCRQueryServlet: getBrowseElementID() got: " + ref);
		StringTokenizer refGet = new StringTokenizer(ref, "@");
		if (refGet.countTokens() < 2)
			throw new MCRException(
					"MCRQueryServlet: Sorry \"ref\" has not 2 Tokens: " + ref);
		String id = refGet.nextToken();
		String host = refGet.nextToken();
		List elements = tempDoc.getRootElement().getChildren(
				MCRXMLContainer.TAG_RESULT);
		org.jdom.Element search = null;
		org.jdom.Element prev = null;
		while (!elements.isEmpty()) {
			search = (Element) elements.get(0);
			if (search.getAttributeValue("id").equals(id)
					&& search.getAttributeValue("host").equals(host))
				if (next) {
					search = (Element) elements.get(1);
					elements.clear();
				} else {
					search = prev;
					elements.clear();
				}
			else {
				prev = search;
				elements.remove(0);
			}
		}

		if (search == null)
			throw new MCRException(
					"MCRQueryServlet: Sorry doesn't found searched document");
		int status = ((search.getAttributeValue(MCRXMLContainer.ATTR_SUCC)
				.equals("true")) ? 1 : 0)
				+ ((search.getAttributeValue(MCRXMLContainer.ATTR_PRED)
						.equals("true")) ? 2 : 0);
		id = search.getAttributeValue("id");
		host = search.getAttributeValue("host");
		String result = new StringBuffer().append(status).append('@')
				.append(id).append('@').append(host).toString();
		LOGGER.info("MCRQueryServlet: getBrowseElementID() returns: " + result);
		return result;
	}

	private final MCRXMLContainer sort(MCRXMLContainer xmlcont, String lang) {
		MCRXMLSortInterface sorter = null;
		try {
			sorter = (MCRXMLSortInterface) (Class.forName(MCR_CONFIG.getString(
					"MCR.XMLSortInterfaceImpl", MCR_STANDARD_SORTER)))
					.newInstance();
		} catch (InstantiationException e) {
			throw new MCRException(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new MCRException(e.getMessage(), e);
		} catch (ClassNotFoundException e) {
			throw new MCRException(e.getMessage(), e);
		}
		if (sorter.getServletContext() == null)
			sorter.setServletContext(getServletContext());
		//MCRXMLSorter sorter=new MCRXMLSorter();
		/*
		 * maybe here should be a property used XPath Expression can be relative
		 * to mcr_result
		 */
		// sorter.addSortKey("./*/*/*/title[lang('"+lang+"')]");
		if (customSort) {
			LOGGER
					.info("MCRQueryServlet: CustomSort enalbed. Sorting inorder: "
							+ inOrder);
			sorter.addSortKey(replString(sortKey, MCR_SORTER_CONFIG_DELIMITER,
					lang), inOrder);
		} else {
			int keynum = Integer.parseInt(MCR_CONFIG.getString(
					MCR_SORTER_CONFIG_PREFIX + ".keys.count", "0"));
			boolean inorder = true;
			for (int key = 1; key <= keynum; key++) {
				// get XPATH Expression and hope it's good, if not exist sort
				// for title
				inorder = MCR_CONFIG.getBoolean(MCR_SORTER_CONFIG_PREFIX
						+ ".keys." + key + ".inorder", true);
				sorter.addSortKey(replString(MCR_CONFIG.getString(
						MCR_SORTER_CONFIG_PREFIX + ".keys." + key,
						"./*/*/*/title[lang('" + lang + "')]"),
						MCR_SORTER_CONFIG_DELIMITER, lang), inorder);
			}
		}
		xmlcont.sort(sorter);
		return xmlcont;
	}

	private static String replString(String parse, String from, String to) {
		StringBuffer result = new StringBuffer(parse);
		if ((result.charAt(0) == '\"')
				&& (result.charAt(result.length() - 1) == '\"')) {
			result.deleteCharAt(result.length() - 1).deleteCharAt(0);
			for (int i = result.toString().indexOf(from); i != -1; i = result
					.toString().indexOf(from)) {
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
			returns.getRootElement().addContent(
					(Element) ((Element) children.get(i)).clone());
			amount--;
		}
		returns.getRootElement().setAttribute("count", "" + children.size())
				.setAttribute("offset", "" + offset).setAttribute("size",
						"" + size);
		return returns;
	}

	private static final boolean isInstanceOfLocal(String host,
			String servletHost, String servletPath, int servletPort) {
		final String confPrefix = "MCR.remoteaccess_";
		String RemoteHost = MCR_CONFIG.getString(confPrefix + host + "_host");
		String queryServletPath = servletPath.substring(0, servletPath
				.lastIndexOf("/"))
				+ "/MCRQueryServlet";
		String remotePath = MCR_CONFIG.getString(confPrefix + host
				+ "_query_servlet");
		int remotePort = Integer.parseInt(MCR_CONFIG.getString(confPrefix
				+ host + "_port"));
		return ((RemoteHost.equals(servletHost))
				&& (remotePath.equals(queryServletPath)) && (servletPort == remotePort)) ? true
				: false;
	}

	private final boolean checkInputParameter(HttpServletRequest request) {
		mode = getProperty(request, "mode");
		query = getProperty(request, "query");
		type = getProperty(request, "type");
		layout = getProperty(request, "layout");
		lang = getProperty(request, "lang");

		//multiple host are allowed
		hosts = request.getParameterValues("hosts");
		att_host = (String) request.getAttribute("hosts");
		//dont't overwrite host if getParameter("hosts") was successful
		host = "";
		if (att_host != null && (hosts == null || hosts.length == 0)) {
			host = att_host;
		} else if (hosts != null && hosts.length > 0) {
			// find a Instance of the local one
			String ServerName = request.getServerName();
			LOGGER
					.info("MCRQueryServlet: Try to map remote request to local one!");
			LOGGER.info("MCRQueryServlet: Local Server Name=" + ServerName);
			StringBuffer hostBf = new StringBuffer();
			for (int i = 0; i < hosts.length; i++) {
				if (!hosts[i].equals("local"))
					//the following replaces a remote request with "local" if
					// needed
					hosts[i] = (isInstanceOfLocal(hosts[i], request
							.getServerName(), request.getServletPath(), request
							.getServerPort())) ? "local" : hosts[i];
				//make a comma seperated list of all hosts
				hostBf.append(",").append(hosts[i]);
			}
			host = hostBf.deleteCharAt(0).toString();
			if (host.indexOf("local") != host.lastIndexOf("local")) {
				LOGGER
						.info("MCRQueryServlet: multiple \"local\" will be removed by MCRQueryResult!");
			}
		}

		view = request.getParameter("view");
		referer = request.getParameter("ref");
		String offsetStr = request.getParameter("offset");
		String sizeStr = request.getParameter("size");
		String max_results = request.getParameter("max_results");
		sortKey = request.getParameter(PARAM_SORT);
		if (sortKey != null) {
			if (request.getParameter(PARAM_IN_ORDER) != null
					&& request.getParameter(PARAM_IN_ORDER).toLowerCase()
							.equals("false"))
				inOrder = false;
			else
				inOrder = true;
			customSort = true;
		} else
			customSort = false;

		if (max_results != null)
			maxresults = Integer.parseInt(max_results);
		offset = 0;
		if (offsetStr != null)
			offset = Integer.parseInt(offsetStr);
		size = 0;
		if (sizeStr != null)
			size = Integer.parseInt(sizeStr);

		if (mode == null) {
			mode = "ResultList";
		}
		if (mode.equals("")) {
			mode = "ResultList";
		}
		if (host == null) {
			host = "local";
		}
		if (host.equals("")) {
			host = "local";
		}
		if (query == null) {
			query = "";
		}
		if (type == null) {
			return false;
		}
		if (type.equals("")) {
			return false;
		}
		if (layout == null) {
			layout = type;
		}
		if (layout.equals("")) {
			layout = type;
		}
		if (lang == null) {
			lang = defaultLang;
		}
		if (lang.equals("")) {
			lang = defaultLang;
		}
		type = type.toLowerCase();

		if (view == null)
			view = "";
		else
			view = view.toLowerCase();

		LOGGER.info("MCRQueryServlet : mode = " + mode);
		LOGGER.info("MCRQueryServlet : type = " + type);
		LOGGER.info("MCRQueryServlet : layout = " + layout);
		LOGGER.info("MCRQueryServlet : hosts = " + host);
		LOGGER.info("MCRQueryServlet : lang = " + lang);
		LOGGER.info("MCRQueryServlet : query = \"" + query + "\"");
		return true;
	}

	private final boolean validateCacheSession(HttpServletRequest request) {
		// check for valid session
		if (mode.equals("CachedResultList")) {
			if (!request.isRequestedSessionIdValid()) {
				//page session timed out
				return false;
			}
			cachedFlag = true;
			mode = "ResultList";
		}
		return true;
	}

	private static final void putResult(MCRQueryCollector collector,
			String host, String type, String query, MCRXMLContainer result) {
		try {
			synchronized (result) {
				collector.collectQueryResults(host, type, query, result);
				result.wait();
			}
		} catch (InterruptedException ignored) {
		}
	}

	private final int getStatus(HttpServletRequest request) {
		int status = (getProperty(request, "status") != null) ? Integer
				.parseInt(getProperty(request, "status")) : 0;
		if (LOGGER.isDebugEnabled()) {
			boolean successor = ((status % 2) == 1) ? true : false;
			boolean predecessor = (((status >> 1) % 2) == 1) ? true : false;
			LOGGER.debug("MCRQueryServlet : status = " + status);
			LOGGER.debug("MCRQueryServlet : predecessor = " + predecessor);
			LOGGER.debug("MCRQueryServlet : successor = " + successor);
		}
		return status;
	}

	private final Document queryClassification(String host, String type,
			String query) {
		String squence = MCR_CONFIG.getString(
				"MCR.classifications_search_sequence", "remote-local");
		MCRXMLContainer resarray = new MCRXMLContainer();
		if (squence.equalsIgnoreCase("local-remote")) {
			putResult(collector, "local", type, query, resarray);
			if (resarray.size() == 0) {
				putResult(collector, host, type, query, resarray);
			}
		} else {
			putResult(collector, host, type, query, resarray);
			if (resarray.size() == 0) {
				putResult(collector, "local", type, query, resarray);
			}
		}
		if (resarray.size() == 0)
			return null;
		return resarray.exportAllToDocument();
	}

	private final Document reSort(Document jdom) throws ServletException,
			IOException, MCRException, JDOMException {
		MCRXMLContainer resarray = new MCRXMLContainer();
		resarray.importElements(jdom);
		if (resarray.size() > 0) {
			//let's do resorting.
			return sort(resarray, lang.toLowerCase()).exportAllToDocument();
		}
		LOGGER.fatal("MCRQueryServlet: Error while RE-sorting JDOM:"
				+ "After import Containersize was ZERO!");
		return jdom;
	}

	private final void browse(HttpServletRequest request,
			HttpServletResponse response, Document jdom)
			throws ServletException, IOException {
		/* change generate new query */
		StringTokenizer refGet = null;
		try {
			refGet = new StringTokenizer(this.getBrowseElementID(jdom, referer,
					view.equals("next")), "@");
		} catch (Exception ex) {
			generateErrorPage(request, response,
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Could not resolve browse origin!", ex, false);
			return;
		}
		if (refGet.countTokens() < 3) {
			generateErrorPage(request, response,
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Could not resolve browse origin!", new MCRException(
							"MCRQueryServlet: Sorry \"refGet\" has not 3 Tokens: "
									+ refGet), false);
			return;
		}
		String StrStatus = refGet.nextToken();
		query = new StringBuffer("/mycoreobject[@ID='").append(
				refGet.nextToken()).append("']").toString();
		host = refGet.nextToken();
		mode = "ObjectMetadata";
		request.setAttribute("mode", mode);
		request.removeAttribute("status");
		request.setAttribute("status", StrStatus);
		request.setAttribute("type", type);
		request.setAttribute("layout", layout);
		request.setAttribute("hosts", host);
		request.setAttribute("lang", lang);
		request.setAttribute("query", query);
		request.setAttribute("view", "done");
		LOGGER.info("MCRQueryServlet: sending to myself:" + "?mode=" + mode
				+ "&status=" + StrStatus + "&type=" + type + "&hosts=" + host
				+ "&lang=" + lang + "&query=" + query);
		doGet(request, response);
		return;
	}
}