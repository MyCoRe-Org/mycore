package org.mycore.frontend.wcms;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRSessionMgr;

public class MCRWCMSLogsResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String target = href.substring(href.indexOf(":") + 1);
        String[] parameter = target.split(":");

        String userId = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        String userClass = MCRSessionMgr.getCurrentSession().get("userClass").toString();

        Element root = MCRWCMSAdminServlet.getRoot("logs", userId, userClass);
        MCRWCMSAdminServlet.generateXML_logs(parameter[0], parameter[1], root);
        return new JDOMSource(root);
    }

}
