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
import javax.xml.transform.TransformerException;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.xml.sax.SAXException;

public class MCRBroadcastingServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    final static Map<String, Element> bcRecList = new HashMap<String, Element>();

    public void init() throws MCRConfigurationException, ServletException {
        super.init();
    }

    public void doGetPost(MCRServletJob job) throws IOException, ServletException, JDOMException, TransformerException, SAXException {

        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        MCRSession session = MCRSessionMgr.getCurrentSession();

        boolean sessionSensitive = "true".equals(request.getParameter("sessionSensitive"));
        
        // get mode and process
        Element answer = null;
        boolean transformByXSL = false;
        if (request.getParameter("mode").equals("addReceiver")) {
            addReceiver(session, sessionSensitive);
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
        return MCRAccessManager.checkPermission("broadcasting") || MCRAccessManager.checkPermission("module-broadcasting", "manage");
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

    private void addReceiver(MCRSession session, boolean sessionSensitive) {
        Element value = getReceiverDetails(session);
        // if user==gast put sessionID, otherwise put username+sessionID
        if (session.getUserInformation().getUserID().equals("gast"))
            bcRecList.put(session.getID(), value);
        else {
            String key = getKey(sessionSensitive, session);
            bcRecList.put(key, value);
        }
    }

    private Element getReceiverDetails(MCRSession session) {
        Element details = new Element("details").addContent(new Element("login").setText(session.getUserInformation().getUserID())).addContent(
                        new Element("ip").setText(session.getCurrentIP())).addContent(new Element("session-id").setText(session.getID()));
        return details;
    }

    public static boolean hasReceived(MCRSession session, boolean sessionSensitive) {
        // if (!cache.isEmpty() && cache.get("bcRecList") != null) {
        String key = getKey(sessionSensitive, session);
        return (session.getUserInformation().getUserID().equals("gast") && bcRecList.get(session.getID()) != null)
                || (!session.getUserInformation().getUserID().equals("gast") && bcRecList.get(key) != null);
    }

    private static String getKey(boolean sessionSensitive, MCRSession session) {
        if (sessionSensitive)
            return session.getUserInformation().getUserID().trim() + session.getID().trim();
        else
            return session.getUserInformation().getUserID().trim();
    }

    public void forwardJDOM(HttpServletRequest request, HttpServletResponse response, Element elem, boolean xslTransformation) throws IOException, TransformerException, SAXException {
        Element root = null;
        if (xslTransformation)
            root = new Element("mcr-module-broadcasting-admin");
        else
            root = new Element("mcr-module-broadcasting");

        root.addContent(elem);

        if (xslTransformation)
            getLayoutService().doLayout(request, response, new MCRJDOMContent(root));
        else
            getLayoutService().sendXML(request, response, new MCRJDOMContent(root));
    }
}
