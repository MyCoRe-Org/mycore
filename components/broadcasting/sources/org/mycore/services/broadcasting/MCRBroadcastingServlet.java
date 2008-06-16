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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRBroadcastingServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    final static Map<String, Element> bcRecList = new HashMap<String, Element>();

    public void init() throws MCRConfigurationException, ServletException {
        super.init();
    }

    public void doGetPost(MCRServletJob job) throws IOException, ServletException, JDOMException {

        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        MCRSession session = MCRSessionMgr.getCurrentSession();

        // get mode and process
        Element answer = null;
        boolean transformByXSL = false;
        if (request.getParameter("mode").equals("hasReceived")) {
            String hasReceived = Boolean.toString(hasReceived(session, request));
            answer = new Element("hasReceived").setText(hasReceived);
        } else if (request.getParameter("mode").equals("addReceiver")) {
            addReceiver(session, request);
            answer = new Element("addReceiver").setText("done");
        } else if (request.getParameter("mode").equals("clearReceiverList") && access()) {
            clearReceiverList();
            answer = getReceiverListAsXML(session);
            transformByXSL = true;
        } else if (request.getParameter("mode").equals("getReceiverList") && access()) {
            answer = getReceiverListAsXML(session);
            transformByXSL = true;
        } else {
            transformByXSL = true;
            answer = new Element("nothingDone");
        }

        // render xml
        forwardJDOM(request, response, answer, transformByXSL);
    }

    private static boolean access() {
        return MCRAccessManager.getAccessImpl().checkPermission("module-broadcasting", "manage");
    }

    private Element getReceiverListAsXML(MCRSession session) {

        Element recListRoot = new Element("receiverList").setAttribute("access", "true");
        for (Map.Entry<String, Element> entry : bcRecList.entrySet()) {
            Element key = new Element("key").setText(entry.getKey());
            Element recValue = entry.getValue();
            recListRoot.addContent(new Element("receiver").addContent(key).addContent(recValue.detach()));
        }
        if (bcRecList.isEmpty()) {
            recListRoot.addContent(new Element("empty"));
        }
        return recListRoot;
    }

    private void clearReceiverList() {
        bcRecList.clear();
    }

    private void addReceiver(MCRSession session, HttpServletRequest request) {
        Element value = getReceiverDetails(session);

        // if user==gast put sessionID, otherwise put username+sessionID
        if (session.getCurrentUserID().equals("gast"))
            bcRecList.put(session.getID(), value);
        else {
            String key = getKey(request, session);
            bcRecList.put(key, value);
        }
    }

    private final Element getReceiverDetails(MCRSession session) {
        Element details = new Element("details").addContent(new Element("login").setText(session.getCurrentUserID())).addContent(
                        new Element("ip").setText(session.getCurrentIP())).addContent(new Element("session-id").setText(session.getID()));
        return details;
    }

    private boolean hasReceived(MCRSession session, HttpServletRequest request) {
        // if (!cache.isEmpty() && cache.get("bcRecList") != null) {
        String key = getKey(request, session);
        if ((session.getCurrentUserID().equals("gast") && bcRecList.get(session.getID()) != null)
                        || (!session.getCurrentUserID().equals("gast") && bcRecList.get(key) != null))
            return true;
        return false;
    }

    private final static String getKey(HttpServletRequest request, MCRSession session) {
        if (request.getParameter("sessionSensitive").equals("true"))
            return session.getCurrentUserID().trim() + session.getID().trim();
        else
            return session.getCurrentUserID().trim();
    }

    public void forwardJDOM(HttpServletRequest request, HttpServletResponse response, Element elem, boolean xslTransformation) throws IOException {

        Element root = null;
        if (xslTransformation)
            root = new Element("mcr-module-broadcasting-admin");
        else
            root = new Element("mcr-module-broadcasting");

        root.addContent(elem);
        Document jdom = new Document(root);

        if (xslTransformation)
            getLayoutService().doLayout(request, response, jdom);
        else
            getLayoutService().sendXML(request, response, jdom);
    }
}
