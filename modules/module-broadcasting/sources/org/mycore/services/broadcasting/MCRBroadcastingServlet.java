/*  											*/
/* Module Broadcasting 1.0, 05-2007  		    */
/* +++++++++++++++++++++++++++++++++++++		*/
/*  											*/
/* Andreas Trappe 	- concept, devel. in misc.  */
/*  											*/
/*  											*/
/*  											*/

package org.mycore.services.broadcasting;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRBroadcastingServlet extends MCRServlet {

	private static final long serialVersionUID = 1L;

	private static Logger LOGGER = Logger
			.getLogger(MCRBroadcastingServlet.class);

	final static MCRCache cache = new MCRCache(1);

	public void init() throws MCRConfigurationException, ServletException {
		super.init();
	}

	public void doGetPost(MCRServletJob job) throws IOException,
			ServletException, JDOMException {

		HttpServletRequest request = job.getRequest();
		HttpServletResponse response = job.getResponse();
		printRequest(request);

		MCRSession session = MCRSessionMgr.getCurrentSession();

		// get mode and process
		Element answer = null;
		if (request.getParameter("mode").equals("hasReceived")) {
			String hasReceived = Boolean.toString(hasReceived(session));
			answer = new Element("hasReceived").setText(hasReceived);
		} else if (request.getParameter("mode").equals("addReceiver")) {
			addReceiver(session);
			answer = new Element("addReceiver").setText("done");
		} else if (request.getParameter("mode").equals("clearReceiverList")) {
			clearReceiverList();
			answer = new Element("clearReceiverList").setText("done");
		} else
			answer = new Element("nothingDone");

		// render xml
		forwardJDOM(request, response, answer);
	}

	private void clearReceiverList() {
		cache.clear();
	}

	private void addReceiver(MCRSession session) {
		HashMap recList;
		if (cache.isEmpty())
			recList = new HashMap();
		else
			recList = (HashMap) cache.get("bcRecList");
		recList.put(session.getCurrentUserID(), "dummy");
		cache.put("bcRecList", recList);
	}

	private boolean hasReceived(MCRSession session) {
		if (!cache.isEmpty() && cache.get("bcRecList") != null) {
			HashMap recList = (HashMap) cache.get("bcRecList");
			if (recList.get(session.getCurrentUserID()) != null)
				return true;
		}
		return false;
	}

	public void forwardJDOM(HttpServletRequest request,
			HttpServletResponse response, Element elem) throws IOException {

		Element root = new Element("mcr-module-broadcasting");
		root.addContent(elem);
		Document jdom = new Document(root);
		getLayoutService().sendXML(request, response, jdom);
	}

	public void printRequest(HttpServletRequest request) {
		LOGGER.debug("############################################# ");
		for (Enumeration e = request.getHeaderNames(); e.hasMoreElements();) {
			String name = (String) (e.nextElement());
			LOGGER.debug("HEADER: " + name + "=" + request.getHeader(name));
		}
		LOGGER.debug("start print Request-Parameters ############## ");
		for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
			String name = (String) (e.nextElement());
			LOGGER.debug("" + name + "=" + request.getParameter(name));
		}
		LOGGER.debug("finished printing Request-Parameters ######### ");
		LOGGER.debug("                                               ");

		LOGGER.debug("start print Request-Attributes ################ ");
		for (Enumeration e = request.getAttributeNames(); e.hasMoreElements();) {
			String name = (String) (e.nextElement());
			LOGGER.debug("" + name + "=" + request.getAttribute(name));
		}
		LOGGER.debug("finished printing Request-Attributes ########## ");
		LOGGER.debug("############################################### ");
	}

	public void prepareErrorPage(HttpServletRequest request,
			HttpServletResponse response, String errorMessage)
			throws IOException, ServletException {
		LOGGER.error(errorMessage);
		generateErrorPage(request, response,
				HttpServletResponse.SC_BAD_REQUEST, errorMessage,
				new MCRException(errorMessage), false);
		return;
	}

}
