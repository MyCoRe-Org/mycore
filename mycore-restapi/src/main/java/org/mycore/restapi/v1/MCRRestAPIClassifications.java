/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.restapi.v1;

import static org.mycore.frontend.jersey.MCRJerseyUtil.APPLICATION_JSON_UTF_8;
import static org.mycore.frontend.jersey.MCRJerseyUtil.APPLICATION_XML_UTF_8;
import static org.mycore.frontend.jersey.MCRJerseyUtil.TEXT_XML_UTF_8;
import static org.mycore.solr.MCRSolrUtils.escapeSearchValue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.management.modelmbean.XMLParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.glassfish.jersey.server.ContainerRequest;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.restapi.v1.errors.MCRRestAPIError;
import org.mycore.restapi.v1.errors.MCRRestAPIException;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

import com.google.gson.JsonIOException;
import com.google.gson.stream.JsonWriter;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

/**
 * REST API for classification objects.
 *
 * @author Robert Stephan
 */
@Path("/classifications")
public class MCRRestAPIClassifications {

    public static final String FORMAT_JSON = "json";

    public static final String FORMAT_XML = "xml";

    private static final MCRCategoryDAO DAO = new MCRCategoryDAOImpl();

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ELEMENT_CATEGORY = "category";

    private static final String ELEMENT_LABEL = "label";

    private static final String ATTRIBUTE_LANG = "lang";

    private static final String ATTRIBUTE_TEXT = "text";

    private static final String ATTRIBUTE_ID = "ID";

    private static final String ATTRIBUTE_HREF = "href";

    private static final String JSON_PROPERTY_TEXT = "text";

    private static final String JSON_PROPERTY_LANG = "lang";

    @Context
    ContainerRequest request;

    private final Date lastModified = new Date(DAO.getLastModified());

    /**
     * Output xml
     * @param eRoot - the root element
     * @param lang - the language which should be filtered or null for no filter
     * @return a string representation of the XML
     */
    private static String writeXML(Element eRoot, String lang) throws IOException {
        StringWriter sw = new StringWriter();
        if (lang != null) {
            // <label xml:lang="en" text="part" />
            XPathExpression<Element> xpE = XPathFactory.instance().compile("//label[@xml:lang!='" + lang + "']",
                Filters.element(), null, Namespace.XML_NAMESPACE);
            for (Element e : xpE.evaluate(eRoot)) {
                e.getParentElement().removeContent(e);
            }
        }
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        Document docOut = new Document(eRoot.detach());
        xout.output(docOut, sw);
        return sw.toString();
    }

    /**
     * output categories in JSON format
     *
     * @param eParent - the parent xml element
     * @param writer - the JSON writer
     * @param lang - the language to be filtered or null if all languages should be displayed
     */
    private static void writeChildrenAsJSON(Element eParent, JsonWriter writer, String lang) throws IOException {
        if (eParent.getChildren(ELEMENT_CATEGORY).isEmpty()) {
            return;
        }
        writer.name("categories");
        writer.beginArray();
        for (Element e : eParent.getChildren(ELEMENT_CATEGORY)) {
            writer.beginObject();
            writer.name("ID").value(e.getAttributeValue(ATTRIBUTE_ID));
            writer.name("labels").beginArray();
            for (Element eLabel : e.getChildren(ELEMENT_LABEL)) {
                if (lang == null || lang.equals(eLabel.getAttributeValue(ATTRIBUTE_LANG, Namespace.XML_NAMESPACE))) {
                    writeLabelAttributesToJson(writer, eLabel);
                }
            }
            writer.endArray();

            if (!e.getChildren(ELEMENT_CATEGORY).isEmpty()) {
                writeChildrenAsJSON(e, writer, lang);
            }
            writer.endObject();
        }
        writer.endArray();
    }

    private static void writeLabelAttributesToJson(JsonWriter writer, Element element) throws IOException {
        writer.beginObject();
        writer.name(JSON_PROPERTY_LANG).value(element.getAttributeValue(ATTRIBUTE_LANG, Namespace.XML_NAMESPACE));
        writer.name(JSON_PROPERTY_TEXT).value(element.getAttributeValue(ATTRIBUTE_TEXT));
        if (element.getAttributeValue("description") != null) {
            writer.name("description").value(element.getAttributeValue("description"));
        }
        writer.endObject();
    }

    /**
     * output children in JSON format used as input for Dijit Checkbox Tree
     *
     * @param eParent - the parent xml element
     * @param writer - the JSON writer
     * @param lang - the language to be filtered or null if all languages should be displayed
     *
     */
    private static void writeChildrenAsJSONCBTree(Element eParent, JsonWriter writer, String lang, boolean checked)
        throws IOException {
        writer.beginArray();
        for (Element e : eParent.getChildren(ELEMENT_CATEGORY)) {
            writer.beginObject();
            writer.name("ID").value(e.getAttributeValue(ATTRIBUTE_ID));
            for (Element eLabel : e.getChildren(ELEMENT_LABEL)) {
                if (lang == null || lang.equals(eLabel.getAttributeValue(ATTRIBUTE_LANG, Namespace.XML_NAMESPACE))) {
                    writer.name(JSON_PROPERTY_TEXT).value(eLabel.getAttributeValue(ATTRIBUTE_TEXT));
                }
            }
            writer.name("checked").value(checked);
            if (!e.getChildren(ELEMENT_CATEGORY).isEmpty()) {
                writer.name("children");
                writeChildrenAsJSONCBTree(e, writer, lang, checked);
            }
            writer.endObject();
        }
        writer.endArray();
    }

    /**
     * output children in JSON format used as input for a jsTree
     *
     * @param eParent - the parent xml element
     * @param writer - the JSON writer
     * @param lang - the language to be filtered or null if all languages should be displayed
     * @param opened - true, if all leaf nodes should be displayed
     * @param disabled - true, if all nodes should be disabled
     * @param selected - true, if all node should be selected
     *
     */
    private static void writeChildrenAsJSONJSTree(Element eParent, JsonWriter writer, String lang, boolean opened,
        boolean disabled, boolean selected) throws IOException {
        writer.beginArray();
        for (Element e : eParent.getChildren(ELEMENT_CATEGORY)) {
            writer.beginObject();
            writer.name("id").value(e.getAttributeValue(ATTRIBUTE_ID));
            for (Element eLabel : e.getChildren(ELEMENT_LABEL)) {
                if (lang == null || lang.equals(eLabel.getAttributeValue(ATTRIBUTE_LANG, Namespace.XML_NAMESPACE))) {
                    writer.name(JSON_PROPERTY_TEXT).value(eLabel.getAttributeValue(ATTRIBUTE_TEXT));
                }
            }
            if (opened || disabled || selected) {
                writer.name("state");
                writer.beginObject();
                if (opened) {
                    writer.name("opened").value(true);
                }
                if (disabled) {
                    writer.name("disabled").value(true);
                }
                if (selected) {
                    writer.name("selected").value(true);
                }
                writer.endObject();
            }
            if (!e.getChildren(ELEMENT_CATEGORY).isEmpty()) {
                writer.name("children");
                writeChildrenAsJSONJSTree(e, writer, lang, opened, disabled, selected);
            }
            writer.endObject();
        }
        writer.endArray();
    }

    /**
     * lists all available classifications as XML or JSON
     *
     * @param info - the URIInfo object
     * @param format - the output format ('xml' or 'json)
     * @return a Jersey Response Object
     */
    @GET
    @Produces({ TEXT_XML_UTF_8, APPLICATION_JSON_UTF_8 })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS))
    public Response listClassifications(@Context UriInfo info,
        @QueryParam("format") @DefaultValue("json") String format) {

        Response.ResponseBuilder builder = request.evaluatePreconditions(lastModified);
        if (builder != null) {
            return builder.build();
        }

        if (FORMAT_XML.equals(format)) {
            StringWriter sw = new StringWriter();

            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            Document docOut = new Document();
            Element eRoot = new Element("mycoreclassifications");
            docOut.setRootElement(eRoot);

            for (MCRCategory cat : DAO.getRootCategories()) {
                String href = info.getAbsolutePathBuilder().path(cat.getId().getRootID()).build().toString();
                Element mycoreclass = new Element("mycoreclass")
                    .setAttribute(ATTRIBUTE_ID, cat.getId().getRootID())
                    .setAttribute(ATTRIBUTE_HREF, href);
                eRoot.addContent(mycoreclass);
            }
            try {
                xout.output(docOut, sw);
                return Response.ok(sw.toString())
                    .lastModified(lastModified)
                    .type(APPLICATION_XML_UTF_8)
                    .build();
            } catch (IOException e) {
                LOGGER.error("Error while writing JSON object to XML", e);
            }
        }

        if (FORMAT_JSON.equals(format)) {
            StringWriter sw = new StringWriter();
            try {
                JsonWriter writer = new JsonWriter(sw);
                writer.setIndent("    ");
                writer.beginObject();
                writer.name("mycoreclass");
                writer.beginArray();
                for (MCRCategory cat : DAO.getRootCategories()) {
                    writer.beginObject();
                    writer.name("ID").value(cat.getId().getRootID());
                    writer.name("href")
                        .value(info.getAbsolutePathBuilder().path(cat.getId().getRootID()).build().toString());
                    writer.endObject();
                }
                writer.endArray();
                writer.endObject();

                writer.close();

                return Response.ok(sw.toString())
                    .type(APPLICATION_JSON_UTF_8)
                    .lastModified(lastModified)
                    .build();
            } catch (IOException e) {
                LOGGER.error("Error while writing JSON object to JSON", e);
            }
        }
        return Response.status(Status.BAD_REQUEST).build();
    }

    /**
     *  returns a single classification object
     *
     * @param classID - the classfication id
     * @param format
     *   Possible values are: json | xml (required)
     * @param filter
     * 	 a ';'-separated list of ':'-separated key-value pairs, possible keys are:
     *      - lang - the language of the returned labels, if ommited all labels in all languages will be returned
     *      - root - an id for a category which will be used as root
     *      - nonempty - hide empty categories
     * @param style
     * 	a ';'-separated list of values, possible keys are:
     *   	- 'checkboxtree' - create a json syntax which can be used as input for a dojo checkboxtree;
     *      - 'checked'   - (together with 'checkboxtree') all checkboxed will be checked
     *      - 'jstree' - create a json syntax which can be used as input for a jsTree
     *      - 'opened' - (together with 'jstree') - all nodes will be opened
     *      - 'disabled' - (together with 'jstree') - all nodes will be disabled
     *      - 'selected' - (together with 'jstree') - all nodes will be selected
     * @param callback - used in JSONP to wrap json result into a Javascript function named by callback parameter
     * @return a Jersey Response object
     */
    @GET
    //@Path("/id/{value}{format:(\\.[^/]+?)?}")  -> working, but returns empty string instead of default value
    @Path("/{classID}")
    @Produces({ TEXT_XML_UTF_8, APPLICATION_JSON_UTF_8 })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS))
    public Response showObject(@PathParam("classID") String classID,
        @QueryParam("format") @DefaultValue("xml") String format, @QueryParam("filter") @DefaultValue("") String filter,
        @QueryParam("style") @DefaultValue("") String style,
        @QueryParam("callback") @DefaultValue("") String callback) {

        Response.ResponseBuilder builder = request.evaluatePreconditions(lastModified);
        if (builder != null) {
            return builder.build();
        }
        try {
            MCRCategory category = getCategoryOrThrow(classID);
            Document doc = MCRCategoryTransformer.getMetaDataDocument(category, false);
            Element rootElement = getRootElement(doc, filter);
            if (filter.contains("nonempty")) {
                final Element root;
                if (rootElement.getName().equals("mycoreclass")) {
                    root = rootElement.getChild("categories");
                } else {
                    root = rootElement;
                }
                filterNonEmpty(doc.getRootElement().getAttributeValue(ATTRIBUTE_ID), root);
            }
            if (filter.contains("nochildren")) {
                rootElement.removeChildren(ELEMENT_CATEGORY);
            }

            return createResponse(format, rootElement, style, callback, filter);
        } catch (MCRRestAPIException e) {
            return Response.status(e.getStatus()).entity(e.getMessage()).build();

        } catch (Exception e) {
            LogManager.getLogger(this.getClass()).error("Error outputting classification", e);
            //TODO response.sendError(HttpServletResponse.SC_NOT_FOUND,
            // "Please specify parameters format and classid.");

            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error outputting classification").build();
        }
    }

    private MCRCategory getCategoryOrThrow(String classID) throws MCRRestAPIException {
        MCRCategory category = DAO.getCategory(new MCRCategoryID(classID), -1);
        if (category == null) {
            throw new MCRRestAPIException(Status.NOT_FOUND,
                new MCRRestAPIError(MCRRestAPIError.CODE_NOT_FOUND, "Classification not found.",
                    "There is no classification with the given ID."));
        }
        return category;
    }

    private Element getRootElement(Document doc, String filter) throws MCRRestAPIException {
        String rootCategoryID = null;
        Element rootElement = doc.getRootElement();
        for (String f : filter.split(";")) {
            if (f.startsWith("root:")) {
                rootCategoryID = f.substring(5);
            }
        }
        if (rootCategoryID != null) {
            XPathExpression<Element> xpe = XPathFactory.instance().compile("//category[@ID='" + rootCategoryID + "']",
                Filters.element());
            Element element = xpe.evaluateFirst(doc);
            if (element != null) {
                rootElement = element;
            } else {
                throw new MCRRestAPIException(Status.NOT_FOUND,
                    new MCRRestAPIError(MCRRestAPIError.CODE_NOT_FOUND, "Category not found.",
                        "The classification does not contain a category with the given ID."));
            }
        }
        return rootElement;
    }

    private Response createResponse(String format, Element rootElement, String style, String callback, String filter)
        throws XMLParseException {
        String lang = null;

        for (String f : filter.split(";")) {
            if (f.startsWith("lang:")) {
                lang = f.substring(5);
            }
        }
        if (FORMAT_JSON.equals(format)) {
            String json;
            //eventually: allow Cross Site Requests: .header("Access-Control-Allow-Origin", "*")
            try {
                json = writeJSON(rootElement, lang, style);
            } catch (IOException e) {
                throw new JsonIOException("failed to create json response", e);
            }
            if (!callback.isEmpty()) {
                return Response.ok(callback + "(" + json + ")")
                    .lastModified(lastModified)
                    .type(APPLICATION_JSON_UTF_8)
                    .build();
            } else {
                return Response.ok(json)
                    .type(APPLICATION_JSON_UTF_8)
                    .build();
            }
        }

        if (FORMAT_XML.equals(format)) {
            String xml;
            try {
                xml = writeXML(rootElement, lang);
            } catch (IOException e) {
                throw new XMLParseException(e, "failed to create xml response");
            }
            return Response.ok(xml)
                .lastModified(lastModified)
                .type(APPLICATION_XML_UTF_8)
                .build();
        }

        return Response.serverError().status(Status.BAD_REQUEST).build();
    }

    /**
     * Output JSON
     * @param eRoot - the category element
     * @param lang - the language to be filtered for or null if all languages should be displayed
     * @param style - the style
     * @return a string representation of a JSON object
     */
    private String writeJSON(Element eRoot, String lang, String style) throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);

        writer.setIndent("  ");

        Element categoriesElement = eRoot.getChild("categories");
        if (style.contains("checkboxtree")) {
            String finalLang = (lang != null) ? lang : "de";
            writer.beginObject();
            writer.name("identifier").value(eRoot.getAttributeValue(ATTRIBUTE_ID));
            for (Element eLabel : eRoot.getChildren(ELEMENT_LABEL)) {
                if (finalLang.equals(eLabel.getAttributeValue(ATTRIBUTE_LANG, Namespace.XML_NAMESPACE))) {
                    writer.name("label").value(eLabel.getAttributeValue(ATTRIBUTE_TEXT));
                }
            }
            writer.name("items");
            writeChildrenAsJSONCBTree(categoriesElement, writer, finalLang, style.contains("checked"));
            writer.endObject();
        } else if (style.contains("jstree")) {
            String finalLang = (lang != null) ? lang : "de";
            writeChildrenAsJSONJSTree(categoriesElement, writer, finalLang, style.contains("opened"),
                style.contains("disabled"), style.contains("selected"));
        } else {
            writer.beginObject(); // {
            writer.name("ID").value(eRoot.getAttributeValue(ATTRIBUTE_ID));
            writer.name("label");
            writer.beginArray();
            for (Element eLabel : eRoot.getChildren(ELEMENT_LABEL)) {
                if (lang == null || lang.equals(eLabel.getAttributeValue(ATTRIBUTE_LANG, Namespace.XML_NAMESPACE))) {
                    writeLabelAttributesToJson(writer, eRoot);
                }
            }
            writer.endArray();
            if (eRoot.equals(eRoot.getDocument().getRootElement())) {
                writeChildrenAsJSON(eRoot.getChild("categories"), writer, lang);
            } else {
                writeChildrenAsJSON(eRoot, writer, lang);
            }

            writer.endObject();
        }
        writer.close();
        return sw.toString();
    }

    private void filterNonEmpty(String classId, Element e) {
        SolrClient solrClient = MCRSolrCoreManager.getMainSolrClient();
        Element[] categories = e.getChildren(ELEMENT_CATEGORY).toArray(Element[]::new);
        for (Element cat : categories) {
            SolrQuery solrQuery = new SolrQuery();
            String q = "category:\"" + escapeSearchValue(classId + ":" + cat.getAttributeValue(ATTRIBUTE_ID)) + "\"";
            solrQuery.setQuery(q);
            solrQuery.setRows(0);
            try {
                QueryRequest queryRequest = new QueryRequest(solrQuery);
                MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(queryRequest,
                    MCRSolrAuthenticationLevel.SEARCH);
                QueryResponse response = queryRequest.process(solrClient);
                SolrDocumentList solrResults = response.getResults();
                if (solrResults.getNumFound() == 0) {
                    cat.detach();
                } else {
                    filterNonEmpty(classId, cat);
                }
            } catch (SolrServerException | IOException exc) {
                LOGGER.error(exc);
            }
        }
    }

}
