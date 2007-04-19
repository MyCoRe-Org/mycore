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
		MCRSession session = MCRSessionMgr.getCurrentSession();

		// get mode and process
		Element answer = null;
		if (request.getParameter("mode").equals("hasReceived")) {
			String hasReceived = Boolean.toString(hasReceived(session, request));
			answer = new Element("hasReceived").setText(hasReceived);
		} else if (request.getParameter("mode").equals("addReceiver")) {
			addReceiver(session, request);
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

	private void addReceiver(MCRSession session, HttpServletRequest request) {
		HashMap recList;
		if (cache.isEmpty())
			recList = new HashMap();
		else
			recList = (HashMap) cache.get("bcRecList");
		
		// if user==gast put sessionID, otherwise put username+sessionID
		if (session.getCurrentUserID().equals("gast")) 
			recList.put(session.getID(), "dummy");	
		else {
			String key = getKey(request, session);
			recList.put(key, "dummy");
		}
		
		cache.put("bcRecList", recList);
	}

	private boolean hasReceived(MCRSession session, HttpServletRequest request) {
		if (!cache.isEmpty() && cache.get("bcRecList") != null) {
			HashMap recList = (HashMap) cache.get("bcRecList");
			
			String key = getKey(request, session);
			if ( (session.getCurrentUserID().equals("gast") && recList.get(session.getID())!=null)					
					|| (!session.getCurrentUserID().equals("gast") && recList.get(key)!=null))
				return true;
		}
		return false;
	}

	private final static String getKey(HttpServletRequest request, MCRSession session) {
		if (request.getParameter("sessionSensitive").equals("true")) 
			return session.getCurrentUserID().trim()+session.getID().trim();
		else 
			return session.getCurrentUserID().trim();
	}
	
	public void forwardJDOM(HttpServletRequest request,
			HttpServletResponse response, Element elem) throws IOException {

		Element root = new Element("mcr-module-broadcasting");
		root.addContent(elem);
		Document jdom = new Document(root);
		getLayoutService().sendXML(request, response, jdom);
	}
}
