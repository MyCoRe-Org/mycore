/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.servlets;

import java.util.Collection;
import java.util.Map;

import org.jdom2.Element;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;

/**
 * This servlet is used to display all parents of an mycore object and their
 * containing derivates. It returns a xml document which will be transformed
 * by derivateLinks-parentList.xsl.
 *  
 * @author Matthias Eichner
 */
public class MCRDerivateLinkServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    protected static String derivateLinkErrorPage = "error_derivatelink.xml";

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String[]> pMap = job.getRequest().getParameterMap();
        String webpage = pMap.get("subselect.webpage")[0];
        String mcrId = getSubParameterValueOfBrowserAddressParameter(webpage, "mcrid");
        String parentId = getSubParameterValueOfBrowserAddressParameter(webpage, "parentID");

        // create a new root element
        Element rootElement = new Element("derivateLinks-parentList");

        MCRObjectID objId = MCRObjectID.getInstance(mcrId);
        if (MCRMetadataManager.exists(objId)) {
            /* mcr object exists in datastore -> add all parent with their
             * derivates to the jdom tree */
            addParentsToElement(rootElement, objId);
        } else if (parentId != null && MCRMetadataManager.exists(MCRObjectID.getInstance(parentId))) {
            /* mcr object doesnt exists in datastore -> use the parent id
             * to create the content */
            Element firstParent = getMyCoReObjectElement(MCRObjectID.getInstance(parentId));
            if (firstParent != null) {
                rootElement.addContent(firstParent);
            }
            addParentsToElement(rootElement, MCRObjectID.getInstance(parentId));
        }

        // check if root element has content -> if not, show an error page
        if (rootElement.getContentSize() == 0) {
            job.getResponse().sendRedirect(
                job.getResponse().encodeRedirectURL(MCRFrontendUtil.getBaseURL() + derivateLinkErrorPage));
            return;
        }

        // set some important attributes to the root element
        rootElement.setAttribute("session", pMap.get("subselect.session")[0]);
        rootElement.setAttribute("varpath", pMap.get("subselect.varpath")[0]);
        rootElement.setAttribute("webpage", webpage);

        // transform & display the generated xml document
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), new MCRJDOMContent(rootElement));
    }

    /**
     * This method adds all parents and their derivates
     * iterative to the given element.
     * 
     * @param toElement the element where the parents and derivates will be added
     * @param objId the source object id
     */
    private void addParentsToElement(Element toElement, MCRObjectID objId) {
        MCRObjectID pId = objId;
        while ((pId = getParentId(pId)) != null) {
            Element parentElement = getMyCoReObjectElement(pId);
            if (parentElement != null) {
                toElement.addContent(parentElement);
            }
        }
    }

    /**
     * Returns the parent object id of an mcr object.
     * 
     * @param objectId from which id
     * @return the parent id
     */
    private MCRObjectID getParentId(MCRObjectID objectId) {
        MCRObject obj = MCRMetadataManager.retrieveMCRObject(objectId);
        return obj.getStructure().getParentID();
    }

    /**
     * Creates a new mcrobject jdom element which contains all
     * derivates with their ids as children.
     * 
     * @param objectId id of the mcr object
     * @return a new jdom element
     */
    private Element getMyCoReObjectElement(MCRObjectID objectId) {
        Collection<String> derivates = MCRLinkTableManager.instance().getDestinationOf(objectId, "derivate");
        if (derivates.size() <= 0) {
            return null;
        }
        Element objElement = new Element("mycoreobject");
        objElement.setAttribute("id", objectId.toString());
        for (String derivate : derivates) {
            Element derElement = new Element("derivate");
            derElement.setAttribute("id", derivate);
            objElement.addContent(derElement);
        }
        return objElement;
    }

    /**
     * Returns the value of a parameter which is embedded in a parameter
     * of the browser address.
     * 
     * @param browserAddressParameter the separated parameter from the browser address
     * @param subParameter the sub parameter to search
     * @return the value of the sub parameter
     */
    private String getSubParameterValueOfBrowserAddressParameter(String browserAddressParameter, String subParameter) {
        String value = null;
        int index = browserAddressParameter.indexOf(subParameter);
        if (index != -1) {
            int startIndex = index + subParameter.length() + 1;
            int endIndex = browserAddressParameter.indexOf("&", index + 1);
            if (endIndex == -1) {
                endIndex = browserAddressParameter.length();
            }
            value = browserAddressParameter.substring(startIndex, endIndex);
        }
        return value;
    }
}
