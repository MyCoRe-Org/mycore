/**
 * 
 */
package org.mycore.frontend.servlets;

import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;

/** Handles getting a classification from the database and converting it to xml view so it can be shown in the browser
 *  for saving. URL for this servlet must be: /servlerts/MCRClassExportServlet?id=...
 * @author Radi Radichev
 * 
 */
public class MCRClassExportServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    public void doGetPost(MCRServletJob job) throws Exception, MCRException {
        try {
            String id = getProperty(job.getRequest(), "id");
            if (id == null)
                throw new MCRException("Classification ID is null!");
            MCRCategoryID catId = MCRCategoryID.rootID(id);
            MCRCategory category = DAO.getCategory(catId, -1);
            if (category == null)
                throw new MCRException("Cannot find classification with id: " + id);
            Document jdom = MCRCategoryTransformer.getMetaDataDocument(category, true);
            getLayoutService().sendXML(job.getRequest(), job.getResponse(), jdom);
        } catch (Exception e) {
            generateErrorPage(job.getRequest(), job.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), e, false);
        }
    }
}
