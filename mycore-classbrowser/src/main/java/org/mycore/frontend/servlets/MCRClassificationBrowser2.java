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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.xml.sax.SAXException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
        MCRQueryAdapter adapter = MCRConfiguration2
            .getOrThrow("MCR.Module-classbrowser.QueryAdapter", MCRConfiguration2::instantiateClass);
        adapter.setFieldName(fieldName);
        return adapter;
    }

    protected void configureQueryAdapter(MCRQueryAdapter queryAdapter, HttpServletRequest req) {
        queryAdapter.configure(req);
    }

    @Override
    public void doGetPost(MCRServletJob job) throws Exception {
        LOGGER.info("ClassificationBrowser finished in {} ms.", MCRUtils.measure(() -> processRequest(job)).toMillis());
    }

    private void processRequest(MCRServletJob job) throws IOException, TransformerException, SAXException {
        HttpServletRequest req = job.getRequest();
        Settings settings = Settings.fromRequest(req);

        LOGGER.info("ClassificationBrowser {}", settings);

        MCRCategoryID id = settings.getCategID()
            .map(categId -> new MCRCategoryID(settings.getClassifID(), categId))
            .orElse(MCRCategoryID.rootID(settings.getClassifID()));
        MCRCategory category = MCRCategoryDAOFactory.getInstance().getCategory(id, 1);
        if (category == null) {
            job.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find category: " + id);
            return;
        }
        Element xml = getClassificationBrowserData(req, settings, category);
        renderToHTML(job, settings, xml);
    }

    private Element getClassificationBrowserData(HttpServletRequest req, Settings settings,
        MCRCategory category) {
        Element xml = new Element("classificationBrowserData");
        xml.setAttribute("classification", settings.getClassifID());
        xml.setAttribute("webpage", settings.getWebpage());
        settings.getParameters().ifPresent(p -> xml.setAttribute("parameters", p));

        Optional<MCRQueryAdapter> queryAdapter = configureQueryAdapter(req, settings, xml);

        Function<MCRCategoryID, String> toIdSearchValue = settings.addClassId() ? MCRCategoryID::toString
            : MCRCategoryID::getID;
        List<Element> data = new ArrayList<>();
        for (MCRCategory child : category.getChildren()) {
            queryAdapter.ifPresent(qa -> qa.setCategory(toIdSearchValue.apply(child.getId())));
            long numResults = queryAdapter
                .filter(qa -> settings.countResults())
                .map(MCRQueryAdapter::getResultCount)
                .orElse(0L);
            if ((settings.removeEmptyLeaves()) && (numResults < 1)) {
                continue;
            }

            Element categoryE = getCategoryElement(child, numResults, settings);
            queryAdapter.ifPresent(qa -> categoryE.setAttribute("query", qa.getQueryAsString()));
            data.add(categoryE);
        }

        String objectType = queryAdapter.map(MCRQueryAdapter::getObjectType).orElse(null);
        countLinks(settings, objectType, category, data);
        sortCategories(settings.getSortBy(), data);
        xml.addContent(data);
        return xml;
    }

    private Element getCategoryElement(MCRCategory category, long numResults,
        Settings settings) {
        Element categoryE = new Element("category");
        if (settings.countResults()) {
            categoryE.setAttribute("numResults", String.valueOf(numResults));
        }

        categoryE.setAttribute("id", category.getId().getID());
        categoryE.setAttribute("children", Boolean.toString(category.hasChildren()));

        if (settings.addUri() && (category.getURI() != null)) {
            categoryE.addContent(new Element("uri").setText(category.getURI().toString()));
        }

        addLabel(settings, category, categoryE);
        return categoryE;
    }

    private Optional<MCRQueryAdapter> configureQueryAdapter(HttpServletRequest req, Settings settings, Element xml) {
        if (settings.countResults() || settings.getField().isPresent()) {
            MCRQueryAdapter queryAdapter = getQueryAdapter(settings.getField().orElse(null));

            configureQueryAdapter(queryAdapter, req);
            if (queryAdapter.getObjectType() != null) {
                xml.setAttribute("objectType", queryAdapter.getObjectType());
            }
            return Optional.of(queryAdapter);
        }
        return Optional.empty();
    }

    /**
     * Add label in current lang, otherwise default lang, optional with
     * description
     */
    private void addLabel(Settings settings, MCRCategory child, Element category) {
        MCRLabel label = child.getCurrentLabel()
            .orElseThrow(() -> new MCRException("Category " + child.getId() + " has no labels."));

        category.addContent(new Element("label").setText(label.getText()));

        // if true, add description
        if (settings.addDescription() && (label.getDescription() != null)) {
            category.addContent(new Element("description").setText(label.getDescription()));
        }
    }

    /** Add link count to each category */
    private void countLinks(Settings settings, String objectType, MCRCategory category,
        List<Element> data) {
        if (!settings.countLinks()) {
            return;
        }
        String objType = objectType;
        if (objType != null && objType.trim().length() == 0) {
            objType = null;
        }

        String classifID = category.getId().getRootID();
        Map<MCRCategoryID, Number> count = MCRCategLinkServiceFactory.getInstance().countLinksForType(category,
            objType, true);
        for (Iterator<Element> it = data.iterator(); it.hasNext();) {
            Element child = it.next();
            MCRCategoryID childID = new MCRCategoryID(classifID, child.getAttributeValue("id"));
            int num = (count.containsKey(childID) ? count.get(childID).intValue() : 0);
            child.setAttribute("numLinks", String.valueOf(num));
            if ((settings.removeEmptyLeaves()) && (num < 1)) {
                it.remove();
            }
        }
    }

    /** Sorts by id, by label in current language, or keeps natural order */
    private void sortCategories(String sortBy, List<Element> data) {
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
     */
    private void renderToHTML(MCRServletJob job, Settings settings, Element xml)
        throws IOException, TransformerException,
        SAXException {
        settings.getStyle()
            .ifPresent(style -> job.getRequest().setAttribute("XSL.Style", style)); // XSL.Style, optional
        MCRServlet.getLayoutService().doLayout(job.getRequest(), job.getResponse(), new MCRJDOMContent(xml));
    }

    @Override
    protected boolean allowCrossDomainRequests() {
        return true;
    }

    public interface MCRQueryAdapter {
        void setFieldName(String fieldname);

        void setRestriction(String text);

        void setCategory(String text);

        String getObjectType();

        void setObjectType(String text);

        long getResultCount();

        String getQueryAsString();

        void configure(HttpServletRequest request);
    }

    private static class Settings {
        private String classifID;

        private String categID;

        private boolean countResults;

        private boolean addClassId;

        private boolean addUri;

        private boolean removeEmptyLeaves;

        private String webpage;

        private String field;

        private String parameters;

        private boolean addDescription;

        private boolean countLinks;

        private String sortBy;

        private String style;

        static Settings fromRequest(HttpServletRequest req) {
            Settings s = new Settings();
            s.classifID = req.getParameter("classification");
            s.categID = req.getParameter("category");
            s.countResults = Boolean.parseBoolean(req.getParameter("countresults"));
            s.addClassId = Boolean.parseBoolean(req.getParameter("addclassid"));
            s.addUri = Boolean.parseBoolean(req.getParameter("adduri"));
            s.removeEmptyLeaves = !MCRUtils.filterTrimmedNotEmpty(req.getParameter("emptyleaves"))
                .map(Boolean::valueOf)
                .orElse(true);
            s.webpage = req.getParameter("webpage");
            s.field = req.getParameter("field");
            s.parameters = req.getParameter("parameters");
            s.addDescription = Boolean.parseBoolean(req.getParameter("adddescription"));
            s.countLinks = Boolean.parseBoolean(req.getParameter("countlinks"));
            s.sortBy = req.getParameter("sortby");
            s.style = req.getParameter("style");
            return s;
        }

        String getSortBy() {
            return sortBy;
        }

        Optional<String> getStyle() {
            return MCRUtils.filterTrimmedNotEmpty(style);
        }

        String getClassifID() {
            return classifID;
        }

        Optional<String> getCategID() {
            return MCRUtils.filterTrimmedNotEmpty(categID);
        }

        boolean countResults() {
            return countResults;
        }

        boolean countLinks() {
            return countLinks;
        }

        boolean addClassId() {
            return addClassId;
        }

        boolean addUri() {
            return addUri;
        }

        boolean removeEmptyLeaves() {
            return removeEmptyLeaves;
        }

        boolean addDescription() {
            return addDescription;
        }

        String getWebpage() {
            return webpage;
        }

        Optional<String> getField() {
            return MCRUtils.filterTrimmedNotEmpty(field);
        }

        Optional<String> getParameters() {
            return MCRUtils.filterTrimmedNotEmpty(parameters);
        }

        @Override
        public String toString() {
            return "Settings{" +
                "classifID='" + classifID + '\'' +
                ", categID='" + categID + '\'' +
                ", countResults=" + countResults +
                ", addClassId=" + addClassId +
                ", addUri=" + addUri +
                ", removeEmptyLeaves=" + removeEmptyLeaves +
                ", webpage='" + webpage + '\'' +
                ", field='" + field + '\'' +
                ", parameters='" + parameters + '\'' +
                ", addDescription=" + addDescription +
                ", countLinks=" + countLinks +
                ", sortBy='" + sortBy + '\'' +
                ", style='" + style + '\'' +
                '}';
        }
    }
}
