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

package org.mycore.restapi.v1;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
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
import org.mycore.restapi.v1.errors.MCRRestAPIError;
import org.mycore.restapi.v1.errors.MCRRestAPIException;
import org.mycore.restapi.v1.utils.MCRJSONWebTokenUtil;
import org.mycore.restapi.v1.utils.MCRRestAPIUtil;
import org.mycore.restapi.v1.utils.MCRRestAPIUtil.MCRRestAPIACLPermission;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrUtils;

import com.google.gson.stream.JsonWriter;

/**
 * REST API for classification objects.
 *
 * @author Robert Stephan
 *
 * @version $Revision: $ $Date: $
 */
@Path("/v1/classifications")
public class MCRRestAPIClassifications {

    private static Logger LOGGER = LogManager.getLogger(MCRRestAPIClassifications.class);

    private static final String HEADER_NAME_AUTHORIZATION = "Authorization";

    public static final String FORMAT_JSON = "json";

    public static final String FORMAT_XML = "xml";

    private static final MCRCategoryDAO DAO = new MCRCategoryDAOImpl();

    /**
     * lists all available classifications as XML or JSON
     * 
     * @param info - the URIInfo object
     * @param request - the HTTPServletRequest object
     * @param format - the output format ('xml' or 'json)
     * @return a Jersey Response Object
     * @throws MCRRestAPIException
     */
    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    public Response listClassifications(@Context UriInfo info, @Context HttpServletRequest request,
        @QueryParam("format") @DefaultValue("json") String format) throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(request, MCRRestAPIACLPermission.READ, "/v1/classifications");

        String authHeader = MCRJSONWebTokenUtil
            .createJWTAuthorizationHeader(MCRJSONWebTokenUtil.retrieveAuthenticationToken(request));
        if (FORMAT_XML.equals(format)) {
            StringWriter sw = new StringWriter();

            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            Document docOut = new Document();
            Element eRoot = new Element("mycoreclassifications");
            docOut.setRootElement(eRoot);

            for (MCRCategory cat : DAO.getRootCategories()) {
                eRoot.addContent(new Element("mycoreclass").setAttribute("ID", cat.getId().getRootID()).setAttribute(
                    "href", info.getAbsolutePathBuilder().path(cat.getId().getRootID()).build().toString()));
            }
            try {
                xout.output(docOut, sw);
                return Response.ok(sw.toString()).type("application/xml; charset=UTF-8")
                    .header(HEADER_NAME_AUTHORIZATION, authHeader).build();
            } catch (IOException e) {
                //ToDo
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

                return Response.ok(sw.toString()).type("application/json; charset=UTF-8")
                    .header(HEADER_NAME_AUTHORIZATION, authHeader).build();
            } catch (IOException e) {
                //toDo
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
     * @param request - the HTTPServletRequestObject
     * @param callback - used in JSONP to wrap json result into a Javascript function named by callback parameter
     * @return a Jersey Response object
     * @throws MCRRestAPIException
     */
    @GET
    //@Path("/id/{value}{format:(\\.[^/]+?)?}")  -> working, but returns empty string instead of default value
    @Path("/{classID}")
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    public Response showObject(@Context HttpServletRequest request, @PathParam("classID") String classID,
        @QueryParam("format") @DefaultValue("xml") String format, @QueryParam("filter") @DefaultValue("") String filter,
        @QueryParam("style") @DefaultValue("") String style, @QueryParam("callback") @DefaultValue("") String callback)
        throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(request, MCRRestAPIACLPermission.READ, "/v1/classifications");
        String rootCateg = null;
        String lang = null;
        boolean filterNonEmpty = false;
        boolean filterNoChildren = false;

        for (String f : filter.split(";")) {
            if (f.startsWith("root:")) {
                rootCateg = f.substring(5);
            }
            if (f.startsWith("lang:")) {
                lang = f.substring(5);
            }
            if (f.startsWith("nonempty")) {
                filterNonEmpty = true;
            }
            if (f.startsWith("nochildren")) {
                filterNoChildren = true;
            }
        }

        if (format == null || classID == null) {
            return Response.serverError().status(Status.BAD_REQUEST).build();
            //TODO response.sendError(HttpServletResponse.SC_NOT_FOUND, 
            //        "Please specify parameters format and classid.");
        }
        try {
            MCRCategory cl = DAO.getCategory(MCRCategoryID.rootID(classID), -1);
            if (cl == null) {
                throw new MCRRestAPIException(Response.Status.BAD_REQUEST,
                    new MCRRestAPIError(MCRRestAPIError.CODE_NOT_FOUND, "Classification not found.",
                        "There is no classification with the given ID."));
            }
            Document docClass = MCRCategoryTransformer.getMetaDataDocument(cl, false);
            Element eRoot = docClass.getRootElement();
            if (rootCateg != null) {
                XPathExpression<Element> xpe = XPathFactory.instance().compile("//category[@ID='" + rootCateg + "']",
                    Filters.element());
                Element e = xpe.evaluateFirst(docClass);
                if (e != null) {
                    eRoot = e;
                } else {
                    throw new MCRRestAPIException(Response.Status.BAD_REQUEST,
                        new MCRRestAPIError(MCRRestAPIError.CODE_NOT_FOUND, "Category not found.",
                            "The classfication does not contain a category with the given ID."));
                }
            }
            if (filterNonEmpty) {
                Element eFilter = eRoot;
                if (eFilter.getName().equals("mycoreclass")) {
                    eFilter = eFilter.getChild("categories");
                }
                filterNonEmpty(docClass.getRootElement().getAttributeValue("ID"), eFilter);
            }
            if (filterNoChildren) {
                eRoot.removeChildren("category");
            }

            String authHeader = MCRJSONWebTokenUtil
                .createJWTAuthorizationHeader(MCRJSONWebTokenUtil.retrieveAuthenticationToken(request));
            if (FORMAT_JSON.equals(format)) {
                String json = writeJSON(eRoot, lang, style);
                //eventually: allow Cross Site Requests: .header("Access-Control-Allow-Origin", "*")
                if (callback.length() > 0) {
                    return Response.ok(callback + "(" + json + ")").type("application/javascript; charset=UTF-8")
                        .build();
                } else {
                    return Response.ok(json).type("application/json; charset=UTF-8")
                        .header(HEADER_NAME_AUTHORIZATION, authHeader).build();
                }
            }

            if (FORMAT_XML.equals(format)) {
                String xml = writeXML(eRoot, lang);
                return Response.ok(xml).type("application/xml; charset=UTF-8")
                    .header(HEADER_NAME_AUTHORIZATION, authHeader).build();
            }
        } catch (Exception e) {
            LogManager.getLogger(this.getClass()).error("Error outputting classification", e);
            //TODO response.sendError(HttpServletResponse.SC_NOT_FOUND, "Error outputting classification");
        }
        return null;
    }

    /**
     * Output xml
     * @param eRoot - the root element
     * @param lang - the language which should be filtered or null for no filter
     * @return a string representation of the XML
     * @throws IOException
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
     * Output JSON
     * @param eRoot - the category element
     * @param lang - the language to be filtered for or null if all languages should be displayed
     * @param style - the style
     * @return a string representation of a JSON object
     * @throws IOException
     */
    private String writeJSON(Element eRoot, String lang, String style) throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.setIndent("  ");
        if (style.contains("checkboxtree")) {
            if (lang == null) {
                lang = "de";
            }
            writer.beginObject();
            writer.name("identifier").value(eRoot.getAttributeValue("ID"));
            for (Element eLabel : eRoot.getChildren("label")) {
                if (lang.equals(eLabel.getAttributeValue("lang", Namespace.XML_NAMESPACE))) {
                    writer.name("label").value(eLabel.getAttributeValue("text"));
                }
            }
            writer.name("items");

            writeChildrenAsJSONCBTree(eRoot = eRoot.getChild("categories"), writer, lang, style.contains("checked"));
            writer.endObject();
        } else if (style.contains("jstree")) {
            if (lang == null) {
                lang = "de";
            }
            writeChildrenAsJSONJSTree(eRoot = eRoot.getChild("categories"), writer, lang, style.contains("opened"),
                style.contains("disabled"), style.contains("selected"));
        } else {
            writer.beginObject(); // {
            writer.name("ID").value(eRoot.getAttributeValue("ID"));
            writer.name("label");
            writer.beginArray();
            for (Element eLabel : eRoot.getChildren("label")) {
                if (lang == null || lang.equals(eLabel.getAttributeValue("lang", Namespace.XML_NAMESPACE))) {
                    writer.beginObject();
                    writer.name("lang").value(eLabel.getAttributeValue("lang", Namespace.XML_NAMESPACE));
                    writer.name("text").value(eLabel.getAttributeValue("text"));
                    if (eLabel.getAttributeValue("description") != null) {
                        writer.name("description").value(eLabel.getAttributeValue("description"));
                    }
                    writer.endObject();
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

    /**
     * output categories in JSON format
     * @param eParent - the parent xml element
     * @param writer - the JSON writer
     * @param lang - the language to be filtered or null if all languages should be displayed
     *
     * @throws IOException
     */
    private static void writeChildrenAsJSON(Element eParent, JsonWriter writer, String lang) throws IOException {
        if (eParent.getChildren("category").size() == 0)
            return;

        writer.name("categories");
        writer.beginArray();
        for (Element e : eParent.getChildren("category")) {
            writer.beginObject();
            writer.name("ID").value(e.getAttributeValue("ID"));
            writer.name("labels").beginArray();
            for (Element eLabel : e.getChildren("label")) {
                if (lang == null || lang.equals(eLabel.getAttributeValue("lang", Namespace.XML_NAMESPACE))) {
                    writer.beginObject();
                    writer.name("lang").value(eLabel.getAttributeValue("lang", Namespace.XML_NAMESPACE));
                    writer.name("text").value(eLabel.getAttributeValue("text"));
                    if (eLabel.getAttributeValue("description") != null) {
                        writer.name("description").value(eLabel.getAttributeValue("description"));
                    }
                    writer.endObject();
                }
            }
            writer.endArray();

            if (e.getChildren("category").size() > 0) {
                writeChildrenAsJSON(e, writer, lang);
            }
            writer.endObject();
        }
        writer.endArray();
    }

    /**
     * output children in JSON format used as input for Dijit Checkbox Tree
     *
     * @param eParent - the parent xml element
     * @param writer - the JSON writer
     * @param lang - the language to be filtered or null if all languages should be displayed
     *
     * @throws IOException
     */
    private static void writeChildrenAsJSONCBTree(Element eParent, JsonWriter writer, String lang, boolean checked)
        throws IOException {
        writer.beginArray();
        for (Element e : eParent.getChildren("category")) {
            writer.beginObject();
            writer.name("ID").value(e.getAttributeValue("ID"));
            for (Element eLabel : e.getChildren("label")) {
                if (lang == null || lang.equals(eLabel.getAttributeValue("lang", Namespace.XML_NAMESPACE))) {
                    writer.name("text").value(eLabel.getAttributeValue("text"));
                }
            }
            writer.name("checked").value(checked);
            if (e.getChildren("category").size() > 0) {
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
     * @throws IOException
     */
    private static void writeChildrenAsJSONJSTree(Element eParent, JsonWriter writer, String lang, boolean opened,
        boolean disabled, boolean selected) throws IOException {
        writer.beginArray();
        for (Element e : eParent.getChildren("category")) {
            writer.beginObject();
            writer.name("id").value(e.getAttributeValue("ID"));
            for (Element eLabel : e.getChildren("label")) {
                if (lang == null || lang.equals(eLabel.getAttributeValue("lang", Namespace.XML_NAMESPACE))) {
                    writer.name("text").value(eLabel.getAttributeValue("text"));
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
            if (e.getChildren("category").size() > 0) {
                writer.name("children");
                writeChildrenAsJSONJSTree(e, writer, lang, opened, disabled, selected);
            }
            writer.endObject();
        }
        writer.endArray();
    }

    private void filterNonEmpty(String classId, Element e) {
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        for (int i = 0; i < e.getChildren("category").size(); i++) {
            Element cat = e.getChildren("category").get(i);

            SolrQuery solrQquery = new SolrQuery();
            solrQquery.setQuery(
                "category:\"" + MCRSolrUtils.escapeSearchValue(classId + ":" + cat.getAttributeValue("ID")) + "\"");
            solrQquery.setRows(0);
            try {
                QueryResponse response = solrClient.query(solrQquery);
                SolrDocumentList solrResults = response.getResults();
                if (solrResults.getNumFound() == 0) {
                    e.removeContent(cat);
                    i--;
                }
            } catch (SolrServerException | IOException exc) {
                LOGGER.error(exc);
            }
        }
        for (int i = 0; i < e.getChildren("category").size(); i++) {
            filterNonEmpty(classId, e.getChildren("category").get(i));
        }
    }
}
