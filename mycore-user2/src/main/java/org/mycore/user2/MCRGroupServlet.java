/*
 * $Id$
 * $Revision: 5697 $ $Date: 16.02.2012 $
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

package org.mycore.user2;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRGroupServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final String LAYOUT_ELEMENT_KEY = MCRGroupServlet.class.getName() + ".layoutElement";

    private boolean groupClassificationsDefined;

    private List<MCRCategoryID> groupCategories;

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRServlet#init()
     */
    @Override
    public void init() throws ServletException {
        super.init();
        groupClassificationsDefined = false;
        groupCategories = new ArrayList<MCRCategoryID>();
        groupCategories.add(MCRUser2Constants.GROUP_CLASSID);
        MCRConfiguration config = MCRConfiguration.instance();
        String groupCategoriesValue = config.getString(MCRUser2Constants.CONFIG_PREFIX+"GroupCategegories", null);
        if (groupCategoriesValue == null) {
            return;
        }
        String[] groupCategoriesSplitted = groupCategoriesValue.split(",");
        for (String groupID : groupCategoriesSplitted) {
            String categoryId = groupID.trim();
            if (categoryId.length() > 0) {
                groupCategories.add(MCRCategoryID.fromString(categoryId));
            }
        }
        groupClassificationsDefined = groupCategories.size() > 1;
    }

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRServlet#think(org.mycore.frontend.servlets.MCRServletJob)
     */
    @Override
    protected void think(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        String action = getProperty(request, "action");
        if ("chooseCategory".equals(action) || !groupClassificationsDefined) {
            chooseCategory(request);
        } else {
            chooseGroupRoot(request);
        }
    }

    private static void chooseGroupRoot(HttpServletRequest request) {
        Element rootElement = getRootElement(request);
        request.setAttribute(LAYOUT_ELEMENT_KEY, new Document(rootElement));
    }

    private static Element getRootElement(HttpServletRequest request) {
        Element rootElement = new Element("groups");
        rootElement.setAttribute("queryParams", request.getQueryString());
        return rootElement;
    }

    private static void chooseCategory(HttpServletRequest request) {
        MCRCategoryID categoryID;
        String categID = getProperty(request, "categID");
        if (categID != null) {
            categoryID = MCRCategoryID.fromString(categID);
        } else {
            String rootID = getProperty(request, "classID");
            categoryID = (rootID == null) ? MCRUser2Constants.GROUP_CLASSID : MCRCategoryID.rootID(rootID);
        }
        Element rootElement = getRootElement(request);
        rootElement.setAttribute("classID", categoryID.getRootID());
        if (!categoryID.isRootID()) {
            rootElement.setAttribute("categID", categoryID.getID());
        }
        request.setAttribute(LAYOUT_ELEMENT_KEY, new Document(rootElement));
    }

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRServlet#render(org.mycore.frontend.servlets.MCRServletJob, java.lang.Exception)
     */
    @Override
    protected void render(MCRServletJob job, Exception ex) throws Exception {
        if (ex != null) {
            //do not handle error here
            throw ex;
        }
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), (Document) job.getRequest().getAttribute(LAYOUT_ELEMENT_KEY));
    }

}
