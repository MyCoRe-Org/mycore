package org.mycore.services.acl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.Filter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

public class MCRACLEditorValidation {
    private static Logger LOGGER = Logger.getLogger(MCRACLEditorValidation.class);

    public static boolean nonExistingRule(String objid, String acpool) {
        MCRAccessStore accessStore = MCRAccessStore.getInstance();
        return !accessStore.existsRule(objid, acpool);
    }

    public static boolean validateInput(Element input) throws IOException, JDOMException {
        Filter posFilter = new Filter() {
            public boolean matches(Object arg0) {
                if (((Element) arg0).getAttributeValue("pos") == null) {
                    return true;
                } else
                    return false;
            }
        };
        for (Iterator it = input.getContent(posFilter).iterator(); it.hasNext();) {
            Element currentElem = (Element) it.next();

            System.out.println("#############################################");
            new XMLOutputter(Format.getPrettyFormat()).output(currentElem, System.out);
            System.out.println("#############################################");

            String objID = currentElem.getChildText("OBJID");
            String acPool = currentElem.getChildText("ACPOOL");

            if (MCRAccessStore.getInstance().existsRule(objID, acPool))
                return false;
        }
        return true;
    }

    public static boolean validateFilter(Element input) throws IOException {
        if (input != null) {
            MCRSession session = MCRSessionMgr.getCurrentSession();

            String objIDFilter = input.getChildText("objid");
            String acpoolFilter = input.getChildText("acpool");
            Map filterMap = new HashMap();

            if (objIDFilter != null) {
                filterMap.put("objid", objIDFilter);
                LOGGER.info("******* ObjID FIlter: " + objIDFilter);
            }

            if (acpoolFilter != null) {
                filterMap.put("acpool", acpoolFilter);
                LOGGER.info("******* AcPool FIlter: " + acpoolFilter);
            }

            if (filterMap.size() > 0)
                session.put("filter", filterMap);

            if (objIDFilter == null && acpoolFilter == null && session.get("filter") != null)
                session.deleteObject("filter");

            System.out.println("#############################################");
            new XMLOutputter(Format.getPrettyFormat()).output(input, System.out);
            System.out.println("#############################################");

            // session.put("filter", "true");
        }
        return true;
    }
}
