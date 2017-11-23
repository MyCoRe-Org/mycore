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

package org.mycore.user2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet is used in the role sub select for when administrate a user.
 * The property <code>MCR.user2.RoleCategories</code> can hold any category IDs
 * that could be possible roots for roles.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRRoleServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final String LAYOUT_ELEMENT_KEY = MCRRoleServlet.class.getName() + ".layoutElement";

    private boolean roleClassificationsDefined;

    private List<MCRCategoryID> roleCategories;

    private MCRCategoryDAO categoryDao;

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRServlet#init()
     */
    @Override
    public void init() throws ServletException {
        super.init();
        roleClassificationsDefined = false;
        roleCategories = new ArrayList<>();
        roleCategories.add(MCRUser2Constants.ROLE_CLASSID);
        MCRConfiguration config = MCRConfiguration.instance();
        String roleCategoriesValue = config.getString(MCRUser2Constants.CONFIG_PREFIX + "RoleCategories", null);
        if (roleCategoriesValue == null) {
            return;
        }
        String[] roleCategoriesSplitted = roleCategoriesValue.split(",");
        for (String roleID : roleCategoriesSplitted) {
            String categoryId = roleID.trim();
            if (categoryId.length() > 0) {
                roleCategories.add(MCRCategoryID.fromString(categoryId));
            }
        }
        roleClassificationsDefined = roleCategories.size() > 1;
        categoryDao = MCRCategoryDAOFactory.getInstance();
    }

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRServlet#think(org.mycore.frontend.servlets.MCRServletJob)
     */
    @Override
    protected void think(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        String action = getProperty(request, "action");
        if ("chooseCategory".equals(action) || !roleClassificationsDefined) {
            chooseCategory(request);
        } else {
            chooseRoleRoot(request);
        }
    }

    private void chooseRoleRoot(HttpServletRequest request) {
        Element rootElement = getRootElement(request);
        rootElement.addContent(getRoleElements());
        request.setAttribute(LAYOUT_ELEMENT_KEY, new Document(rootElement));
    }

    private Collection<Element> getRoleElements() {
        ArrayList<Element> list = new ArrayList<>(roleCategories.size());
        for (MCRCategoryID categID : roleCategories) {
            Element role = new Element("role");
            role.setAttribute("categID", categID.toString());
            MCRCategory category = categoryDao.getCategory(categID, 0);
            if (category == null) {
                continue;
            }
            role.setAttribute("label", category.getCurrentLabel().map(MCRLabel::getText).orElse(categID.getID()));
            list.add(role);
        }
        return list;
    }

    private static Element getRootElement(HttpServletRequest request) {
        Element rootElement = new Element("roles");
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
            categoryID = (rootID == null) ? MCRUser2Constants.ROLE_CLASSID : MCRCategoryID.rootID(rootID);
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
        getLayoutService().doLayout(job.getRequest(), job.getResponse(),
            new MCRJDOMContent((Document) job.getRequest().getAttribute(LAYOUT_ELEMENT_KEY)));
    }

}
