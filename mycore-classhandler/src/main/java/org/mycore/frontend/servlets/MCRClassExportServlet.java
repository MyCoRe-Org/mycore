/**
 * 
 */
package org.mycore.frontend.servlets;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRJDOMContent;
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
            String categid = getProperty(job.getRequest(), "categid");
            String id = getProperty(job.getRequest(), "id");

            if (categid == null || categid.length() == 0) {

                if (id == null)
                    throw new MCRException("Classification ID is null!");
                MCRCategoryID catId = MCRCategoryID.rootID(id);
                MCRCategory category = DAO.getCategory(catId, -1);
                if (category == null)
                    throw new MCRException("Cannot find classification with id: " + id);
                Document jdom = MCRCategoryTransformer.getMetaDataDocument(category, true);
                getLayoutService().sendXML(job.getRequest(), job.getResponse(), new MCRJDOMContent(jdom));
            } else {
                Document snippet = getParentIdentifiers(id, categid);
                getLayoutService().sendXML(job.getRequest(), job.getResponse(), new MCRJDOMContent(snippet));
            }
        } catch (Exception e) {
            generateErrorPage(job.getRequest(), job.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), e, false);
        }
    }

    /**
     * Returns the identifiers of all parent categories.
     * 
     * @param classification the identifier of the classification
     * @param categid the category identifier for getting the parent ids for
     * @return
     * @throws Exception
     * 
     * TODO cache generated snippets
     */
    @SuppressWarnings("unchecked")
    private Document getParentIdentifiers(String classification, String categid) throws Exception {
        MCRCategoryID catId = MCRCategoryID.rootID(classification);
        MCRCategory category = DAO.getCategory(catId, -1);
        Document jdom = MCRCategoryTransformer.getMetaDataDocument(category, true);
        XPath xp = XPath.newInstance("/mycoreclass/categories//category[@ID='" + categid + "']/ancestor::category/@ID");
        List<Attribute> nodes = xp.selectNodes(jdom);

        Element identifiers = new Element("identifiers");
        identifiers.setAttribute("classification", classification);
        identifiers.setAttribute("parents-for-categid", categid);

        Document parents = new Document(identifiers);

        for (Attribute attr : nodes) {
            identifiers.addContent(new Element("id").setText(attr.getValue()));
        }

        return parents;
    }
}
