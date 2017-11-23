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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.xml.sax.SAXException;

/**
 * This servlet provides a way to visually navigate through the tree of
 * categories of a classification. The XML output is transformed to HTML
 * using classificationBrowserData.xsl on the server side, then sent to
 * the client browser, where AJAX does the rest.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRClassificationBrowser2 extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger(MCRClassificationBrowser2.class);

    protected MCRQueryAdapter getQueryAdapter(final String fieldName) {
        MCRQueryAdapter adapter = MCRConfiguration.instance().getInstanceOf("MCR.Module-classbrowser.QueryAdapter");
        adapter.setFieldName(fieldName);
        return adapter;
    }

    protected void configureQueryAdapter(MCRQueryAdapter queryAdapter, HttpServletRequest req) {
        queryAdapter.configure(req);
    }

    @Override
    public void doGetPost(MCRServletJob job) throws Exception {
        long time = System.nanoTime();

        HttpServletRequest req = job.getRequest();

        String classifID = req.getParameter("classification");
        String categID = req.getParameter("category");

        boolean countResults = Boolean.valueOf(req.getParameter("countresults"));
        boolean addClassId = Boolean.valueOf(req.getParameter("addclassid"));
        boolean uri = Boolean.valueOf(req.getParameter("adduri"));

        String el = req.getParameter("emptyleaves");
        boolean emptyLeaves = true;
        if ((el != null) && (el.trim().length() > 0)) {
            emptyLeaves = Boolean.valueOf(el);
        }

        LOGGER.info("ClassificationBrowser {} {}", classifID, categID == null ? "" : categID);

        MCRCategoryID id = new MCRCategoryID(classifID, categID);
        Element xml = new Element("classificationBrowserData");
        xml.setAttribute("classification", classifID);
        xml.setAttribute("webpage", req.getParameter("webpage"));

        MCRQueryAdapter queryAdapter = null;

        String field = req.getParameter("field");
        if (countResults || (field != null && field.length() > 0)) {
            queryAdapter = getQueryAdapter(field);

            configureQueryAdapter(queryAdapter, req);
            if (queryAdapter.getObjectType() != null) {
                xml.setAttribute("objectType", queryAdapter.getObjectType());
            }
        }

        String parameters = req.getParameter("parameters");
        if (parameters != null) {
            xml.setAttribute("parameters", parameters);
        }

        List<Element> data = new ArrayList<>();
        MCRCategory category = MCRCategoryDAOFactory.getInstance().getCategory(id, 1);
        if (category == null) {
            job.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find category: " + id);
            return;
        }
        for (MCRCategory child : category.getChildren()) {
            String childID = child.getId().getID();
            long numResults = 0;
            if (queryAdapter != null) {
                queryAdapter.setCategory(addClassId ? child.getId().toString() : childID);
            }
            if (countResults) {
                numResults = queryAdapter.getResultCount();
                if ((!emptyLeaves) && (numResults < 1)) {
                    continue;
                }
            }

            Element categoryE = new Element("category");
            data.add(categoryE);
            if (countResults) {
                categoryE.setAttribute("numResults", String.valueOf(numResults));
            }

            categoryE.setAttribute("id", childID);
            categoryE.setAttribute("children", Boolean.toString(child.hasChildren()));
            if (queryAdapter != null) {
                categoryE.setAttribute("query", queryAdapter.getQueryAsString());
            }

            if (uri && (child.getURI() != null)) {
                categoryE.addContent(new Element("uri").setText(child.getURI().toString()));
            }

            addLabel(req, child, categoryE);
        }

        String objectType = queryAdapter == null ? null : queryAdapter.getObjectType();
        countLinks(req, emptyLeaves, objectType, category, data);
        sortCategories(req, data);
        xml.addContent(data);
        renderToHTML(job, req, xml);

        time = (System.nanoTime() - time) / 1000000;
        LOGGER.info("ClassificationBrowser finished in {} ms", time);
    }

    /**
     * Add label in current lang, otherwise default lang, optional with
     * description
     */
    private void addLabel(HttpServletRequest req, MCRCategory child, Element category) {
        MCRLabel label = child.getCurrentLabel()
            .orElseThrow(() -> new MCRException("Category " + child.getId() + " has no labels."));

        category.addContent(new Element("label").setText(label.getText()));

        // if true, add description
        boolean descr = Boolean.valueOf(req.getParameter("adddescription"));
        if (descr && (label.getDescription() != null)) {
            category.addContent(new Element("description").setText(label.getDescription()));
        }
    }

    /** Add link count to each category */
    private void countLinks(HttpServletRequest req, boolean emptyLeaves, String objectType, MCRCategory category,
        List<Element> data) {
        if (!Boolean.valueOf(req.getParameter("countlinks"))) {
            return;
        }
        if (objectType != null && objectType.trim().length() == 0) {
            objectType = null;
        }

        String classifID = category.getId().getRootID();
        Map<MCRCategoryID, Number> count = MCRCategLinkServiceFactory.getInstance().countLinksForType(category,
            objectType, true);
        for (Iterator<Element> it = data.iterator(); it.hasNext();) {
            Element child = it.next();
            MCRCategoryID childID = new MCRCategoryID(classifID, child.getAttributeValue("id"));
            int num = (count.containsKey(childID) ? count.get(childID).intValue() : 0);
            child.setAttribute("numLinks", String.valueOf(num));
            if ((!emptyLeaves) && (num < 1)) {
                it.remove();
            }
        }
    }

    /** Sorts by id, by label in current language, or keeps natural order */
    private void sortCategories(HttpServletRequest req, List<Element> data) {
        final String sortBy = req.getParameter("sortby");
        switch (sortBy) {
            case "id":
                data.sort(Comparator.comparing(e -> e.getAttributeValue("id")));
                break;
            case "label":
                data.sort(Comparator.comparing(e -> e.getChildText("label"), String::compareToIgnoreCase));
                break;
            default:
                //no sort;
        }
    }

    /** Sends output to client browser 
     * @throws SAXException 
     * @throws TransformerException */
    private void renderToHTML(MCRServletJob job, HttpServletRequest req, Element xml)
        throws IOException, TransformerException,
        SAXException {
        String style = req.getParameter("style"); // XSL.Style, optional
        if ((style != null) && (style.length() > 0)) {
            req.setAttribute("XSL.Style", style);
        }

        MCRServlet.getLayoutService().doLayout(req, job.getResponse(), new MCRJDOMContent(xml));
    }

    @Override
    protected boolean allowCrossDomainRequests() {
        return true;
    }

    public interface MCRQueryAdapter {
        void setFieldName(String fieldname);

        void setRestriction(String text);

        void setCategory(String text);

        void setObjectType(String text);

        String getObjectType();

        long getResultCount();

        String getQueryAsString() throws UnsupportedEncodingException;

        void configure(HttpServletRequest request);
    }
}
