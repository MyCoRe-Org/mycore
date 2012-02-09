package org.mycore.services.broadcasting;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

public class MCRHasReceivedResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String target = href.substring(href.indexOf(":") + 1);
        boolean sessionSensitive = "true".equals(target);
        MCRSession session = MCRSessionMgr.getCurrentSession();
        return new JDOMSource(MCRBroadcastingServlet.getReceived(session, sessionSensitive));
    }

}
