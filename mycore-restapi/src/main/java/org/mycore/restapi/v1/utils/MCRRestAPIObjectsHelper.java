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

package org.mycore.restapi.v1.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.Comment;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.MCRPathXML;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.restapi.v1.MCRRestAPIObjects;
import org.mycore.restapi.v1.errors.MCRRestAPIError;
import org.mycore.restapi.v1.errors.MCRRestAPIException;
import org.mycore.restapi.v1.utils.MCRRestAPISortObject.SortOrder;
import org.mycore.solr.MCRSolrClientFactory;
import org.xml.sax.SAXException;

import com.google.gson.stream.JsonWriter;

/**
 * main utility class that handles REST requests
 * 
 * to filter the XML output of showMCRObject, set the properties:
 * MCR.RestAPI.v1.Filter.XML
 *   to your ContentTransformer-ID,
 * MCR.ContentTransformer.[your ContentTransformer-ID here].Class
 *   to your ContentTransformer's class and
 * MCR.ContentTransformer.[your ContentTransformer-ID here].Stylesheet
 *   to your filtering stylesheet.
 * 
 * @author Robert Stephan
 * @author Christoph Neidahl
 * 
 * @version $Revision: $ $Date: $
 */
public class MCRRestAPIObjectsHelper {

    private static final String GENERAL_ERROR_MSG = "A problem occured while fetching the data.";

    private static Logger LOGGER = LogManager.getLogger(MCRRestAPIObjectsHelper.class);

    private static SimpleDateFormat SDF_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    public static Response showMCRObject(String pathParamId, String queryParamStyle, UriInfo info)
        throws MCRRestAPIException {

        MCRObject mcrObj = retrieveMCRObject(pathParamId);
        Document doc = mcrObj.createXML();
        Element eStructure = doc.getRootElement().getChild("structure");

        if (queryParamStyle != null && !MCRRestAPIObjects.STYLE_DERIVATEDETAILS.equals(queryParamStyle)) {
            throw new MCRRestAPIException(Response.Status.BAD_REQUEST,
                new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_PARAMETER,
                    "The value of parameter {style} is not allowed.",
                    "Allowed values for {style} parameter are: " + MCRRestAPIObjects.STYLE_DERIVATEDETAILS));
        }

        if (MCRRestAPIObjects.STYLE_DERIVATEDETAILS.equals(queryParamStyle) && eStructure != null) {
            Element eDerObjects = eStructure.getChild("derobjects");
            if (eDerObjects != null) {
                for (Element eDer : eDerObjects.getChildren("derobject")) {
                    String derID = eDer.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
                    try {
                        MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derID));
                        eDer.addContent(der.createXML().getRootElement().detach());

                        //<mycorederivate xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xlink="http://www.w3.org/1999/xlink" xsi:noNamespaceSchemaLocation="datamodel-derivate.xsd" ID="cpr_derivate_00003760" label="display_image" version="1.3">
                        //  <derivate display="true">

                        eDer = eDer.getChild("mycorederivate").getChild("derivate");
                        Document docContents = listDerivateContentAsXML(
                            MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derID)), "/", -1, info);
                        if (docContents.hasRootElement()) {
                            eDer.addContent(docContents.getRootElement().detach());
                        }
                    } catch (MCRException e) {
                        eDer.addContent(new Comment("Error: Derivate not found."));
                    } catch (IOException e) {
                        eDer.addContent(new Comment("Error: Derivate content could not be listed: " + e.getMessage()));
                    }
                }
            }
        }

        StringWriter sw = new StringWriter();
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        try {
            String filter_id = MCRConfiguration.instance().getString("MCR.RestAPI.v1.Filter.XML","");
            if (filter_id.length() > 0) {
                MCRContentTransformer trans = MCRContentTransformerFactory.getTransformer(filter_id);
                Document filtered_doc = trans.transform(new MCRJDOMContent(doc)).asXML();
                outputter.output(filtered_doc, sw);
            } else {
                outputter.output(doc, sw);
            }
        } catch (SAXException | JDOMException e) {
            throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR, new MCRRestAPIError(
                MCRRestAPIError.CODE_INTERNAL_ERROR, "Unable to transform MCRContent to XML document", e.getMessage()));
        } catch (IOException e) {
            throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR, new MCRRestAPIError(
                MCRRestAPIError.CODE_INTERNAL_ERROR, "Unable to retrieve/transform MyCoRe object", e.getMessage()));
        }

        return Response.ok(sw.toString())
            .type("application/xml")
            .build();
    }

    public static Response showMCRDerivate(String pathParamMcrID, String pathParamDerID, UriInfo info,
        boolean withDetails) throws MCRRestAPIException {

        MCRObjectID mcrObj = MCRObjectID.getInstance(pathParamMcrID);
        MCRDerivate derObj = retrieveMCRDerivate(mcrObj, pathParamDerID);

        try {
            Document doc = derObj.createXML();
            if (withDetails) {
                Document docContent = listDerivateContentAsXML(derObj, "/", -1, info);
                if (docContent != null && docContent.hasRootElement()) {
                    doc.getRootElement().addContent(docContent.getRootElement().detach());
                }
            }

            StringWriter sw = new StringWriter();
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            outputter.output(doc, sw);
            
            return Response.ok(sw.toString())
                .type("application/xml")
                .build();
        } catch (IOException e) {
            throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
                new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR, GENERAL_ERROR_MSG, e.getMessage()));
        }

        // return MCRRestAPIError.create(Response.Status.INTERNAL_SERVER_ERROR, "Unexepected program flow termination.",
        //       "Please contact a developer!").createHttpResponse();
    }

    private static String listDerivateContentAsJson(MCRDerivate derObj, String path, int depth, UriInfo info)
        throws IOException {
        StringWriter sw = new StringWriter();
        MCRPath root = MCRPath.getPath(derObj.getId().toString(), "/");
        root = MCRPath.toMCRPath(root.resolve(path));
        if (depth == -1) {
            depth = Integer.MAX_VALUE;
        }
        if (root != null) {
            JsonWriter writer = new JsonWriter(sw);
            Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), depth,
                new MCRJSONFileVisitor(writer, derObj.getOwnerID(), derObj.getId(), info));
            writer.close();
        }
        return sw.toString();
    }

    private static Document listDerivateContentAsXML(MCRDerivate derObj, String path, int depth, UriInfo info)
        throws IOException {
        Document doc = new Document();

        MCRPath root = MCRPath.getPath(derObj.getId().toString(), "/");
        root = MCRPath.toMCRPath(root.resolve(path));
        if (depth == -1) {
            depth = Integer.MAX_VALUE;
        }
        if (root != null) {
            Element eContents = new Element("contents");
            eContents.setAttribute("mycoreobject", derObj.getOwnerID().toString());
            eContents.setAttribute("mycorederivate", derObj.getId().toString());
            doc.addContent(eContents);
            if (!path.endsWith("/")) {
                path += "/";
            }
            MCRPath p = MCRPath.getPath(derObj.getId().toString(), path);
            if (p != null && Files.exists(p)) {
                Element eRoot = MCRPathXML.getDirectoryXML(p).getRootElement();
                eContents.addContent(eRoot.detach());
                createXMLForSubdirectories(p, eRoot, 1, depth);
            }

            //add href Attributes
            String baseURL = MCRJerseyUtil.getBaseURL(info)
                + MCRConfiguration.instance().getString("MCR.RestAPI.v1.Files.URL.path");
            baseURL = baseURL.replace("${mcrid}", derObj.getOwnerID().toString()).replace("${derid}",
                derObj.getId().toString());
            XPathExpression<Element> xp = XPathFactory.instance().compile(".//child[@type='file']", Filters.element());
            for (Element e : xp.evaluate(eContents)) {
                String uri = e.getChildText("uri");
                if (uri != null) {
                    int pos = uri.lastIndexOf(":/");
                    String subPath = uri.substring(pos + 2);
                    while (subPath.startsWith("/")) {
                        subPath = path.substring(1);
                    }
                    e.setAttribute("href", baseURL + subPath);
                }
            }
        }
        return doc;
    }

    private static void createXMLForSubdirectories(MCRPath mcrPath, Element currentElement, int currentDepth,
        int maxDepth) {
        if (currentDepth < maxDepth) {
            XPathExpression<Element> xp = XPathFactory.instance().compile("./children/child[@type='directory']",
                Filters.element());
            for (Element e : xp.evaluate(currentElement)) {
                String name = e.getChildTextNormalize("name");
                try {
                    MCRPath pChild = (MCRPath) mcrPath.resolve(name);
                    Document doc = MCRPathXML.getDirectoryXML(pChild);
                    Element eChildren = doc.getRootElement().getChild("children");
                    if (eChildren != null) {
                        e.addContent(eChildren.detach());
                        createXMLForSubdirectories(pChild, e, currentDepth + 1, maxDepth);
                    }
                } catch (IOException ex) {
                    //ignore
                }

            }
        }
    }

    /**
     * returns a list of objects
     * @param info - the injected Jersey URIInfo object
     *
     * @param format - the output format ('xml'|'json')
     * @param filter - a filter criteria
     * @param sort - the sort criteria
     *
     * @return a Jersey response object
     * @throws MCRRestAPIException    
     * 
     * @see MCRRestAPIObjects#listObjects(UriInfo, String, String, String)
     * 
     */
    public static Response listObjects(UriInfo info, String format, String filter,
        String sort) throws MCRRestAPIException {
        List<MCRRestAPIError> errors = new ArrayList<>();
        //analyze sort
        MCRRestAPISortObject sortObj = null;
        try {
            sortObj = createSortObject(sort);
        } catch (MCRRestAPIException rae) {
            errors.addAll(rae.getErrors());
        }

        //analyze format

        if (format.equals(MCRRestAPIObjects.FORMAT_JSON) || format.equals(MCRRestAPIObjects.FORMAT_XML)) {
            //ok
        } else {
            errors.add(new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_PARAMETER, "The parameter 'format' is wrong.",
                "Allowed values for format are 'json' or 'xml'."));
        }

        //analyze filter
        List<String> projectIDs = new ArrayList<>();
        List<String> typeIDs = new ArrayList<>();
        String lastModifiedBefore = null;
        String lastModifiedAfter = null;
        if (filter != null) {
            for (String s : filter.split(";")) {
                if (s.startsWith("project:")) {
                    projectIDs.add(s.substring(8));
                    continue;
                }
                if (s.startsWith("type:")) {
                    typeIDs.add(s.substring(5));
                    continue;
                }
                if (s.startsWith("lastModifiedBefore:")) {
                    if (!validateDateInput(s.substring(19))) {
                        errors.add(new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_PARAMETER,
                            "The parameter 'filter' is wrong.",
                            "The value of lastModifiedBefore could not be parsed. Please use UTC syntax: yyyy-MM-dd'T'HH:mm:ss'Z'."));
                        continue;
                    }
                    if (lastModifiedBefore == null) {
                        lastModifiedBefore = s.substring(19);
                    } else if (s.substring(19).compareTo(lastModifiedBefore) < 0) {
                        lastModifiedBefore = s.substring(19);
                    }
                    continue;
                }

                if (s.startsWith("lastModifiedAfter:")) {
                    if (!validateDateInput(s.substring(18))) {
                        errors.add(new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_PARAMETER,
                            "The parameter 'filter' is wrong.",
                            "The value of lastModifiedAfter could not be parsed. Please use UTC syntax: yyyy-MM-dd'T'HH:mm:ss'Z'."));
                        continue;
                    }
                    if (lastModifiedAfter == null) {
                        lastModifiedAfter = s.substring(18);
                    } else if (s.substring(18).compareTo(lastModifiedAfter) > 0) {
                        lastModifiedAfter = s.substring(18);
                    }
                    continue;
                }

                errors.add(new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_PARAMETER, "The parameter 'filter' is wrong.",
                    "The syntax of the filter '" + s
                        + "'could not be parsed. The syntax should be [filterName]:[value]. Allowed filterNames are 'project', 'type', 'lastModifiedBefore' and 'lastModifiedAfter'."));
            }
        }

        if (errors.size() > 0) {
            throw new MCRRestAPIException(Status.BAD_REQUEST, errors);
        }
        //Parameters are validated - continue to retrieve data

        //retrieve MCRIDs by Type and Project ID
        Set<String> mcrIDs = new HashSet<>();
        if (projectIDs.isEmpty()) {
            if (typeIDs.isEmpty()) {
                mcrIDs = MCRXMLMetadataManager.instance().listIDs().stream().filter(id -> !id.contains("_derivate_"))
                    .collect(Collectors.toSet());
            } else {
                for (String t : typeIDs) {
                    mcrIDs.addAll(MCRXMLMetadataManager.instance().listIDsOfType(t));
                }
            }
        } else {

            if (typeIDs.isEmpty()) {
                for (String id : MCRXMLMetadataManager.instance().listIDs()) {
                    String[] split = id.split("_");
                    if (!split[1].equals("derivate") && projectIDs.contains(split[0])) {
                        mcrIDs.add(id);
                    }
                }
            } else {
                for (String p : projectIDs) {
                    for (String t : typeIDs) {
                        mcrIDs.addAll(MCRXMLMetadataManager.instance().listIDsForBase(p + "_" + t));
                    }
                }
            }
        }

        //Filter by modifiedBefore and modifiedAfter
        List<String> l = new ArrayList<>();
        l.addAll(mcrIDs);
        List<MCRObjectIDDate> objIdDates = new ArrayList<>();
        try {
            objIdDates = MCRXMLMetadataManager.instance().retrieveObjectDates(l);
        } catch (IOException e) {
            //TODO
        }
        if (lastModifiedAfter != null || lastModifiedBefore != null) {
            List<MCRObjectIDDate> testObjIdDates = objIdDates;
            objIdDates = new ArrayList<>();
            for (MCRObjectIDDate oid : testObjIdDates) {
                String test = SDF_UTC.format(oid.getLastModified());
                if (lastModifiedAfter != null && test.compareTo(lastModifiedAfter) < 0)
                    continue;
                if (lastModifiedBefore != null
                    && lastModifiedBefore.compareTo(test.substring(0, lastModifiedBefore.length())) < 0)
                    continue;
                objIdDates.add(oid);
            }
        }

        //sort if necessary
        if (sortObj != null) {
            objIdDates.sort(new MCRRestAPISortObjectComparator(sortObj));
        }

        //output as XML
        if (MCRRestAPIObjects.FORMAT_XML.equals(format)) {
            Element eMcrobjects = new Element("mycoreobjects");
            Document docOut = new Document(eMcrobjects);
            eMcrobjects.setAttribute("numFound", Integer.toString(objIdDates.size()));
            for (MCRObjectIDDate oid : objIdDates) {
                Element eMcrObject = new Element("mycoreobject");
                eMcrObject.setAttribute("ID", oid.getId());
                eMcrObject.setAttribute("lastModified", SDF_UTC.format(oid.getLastModified()));
                eMcrObject.setAttribute("href", info.getAbsolutePathBuilder().path(oid.getId()).build().toString());

                eMcrobjects.addContent(eMcrObject);
            }
            try {
                StringWriter sw = new StringWriter();
                XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
                xout.output(docOut, sw);
                return Response.ok(sw.toString())
                    .type("application/xml; charset=UTF-8")
                    .build();
            } catch (IOException e) {
                throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
                    new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR, GENERAL_ERROR_MSG, e.getMessage()));
            }
        }

        //output as JSON
        if (MCRRestAPIObjects.FORMAT_JSON.equals(format)) {
            StringWriter sw = new StringWriter();
            try {
                JsonWriter writer = new JsonWriter(sw);
                writer.setIndent("    ");
                writer.beginObject();
                writer.name("numFound").value(objIdDates.size());
                writer.name("mycoreobjects");
                writer.beginArray();
                for (MCRObjectIDDate oid : objIdDates) {
                    writer.beginObject();
                    writer.name("ID").value(oid.getId());
                    writer.name("lastModified").value(SDF_UTC.format(oid.getLastModified()));
                    writer.name("href").value(info.getAbsolutePathBuilder().path(oid.getId()).build().toString());
                    writer.endObject();
                }
                writer.endArray();
                writer.endObject();

                writer.close();
                return Response.ok(sw.toString())
                    .type("application/json; charset=UTF-8")
                    .build();
            } catch (IOException e) {
                throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
                    new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR, GENERAL_ERROR_MSG, e.getMessage()));
            }
        }
        throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
            new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR, "A problem in programm flow", null));
    }

    /**
     * returns a list of derivate objects
     * @param info - the injected Jersey URIInfo object
     *
     * @param mcrObjID - the MyCoRe Object ID
     * @param format - the output format ('xml'|'json')
     * @param sort - the sort criteria
     *
     * @return a Jersey response object
     * @throws MCRRestAPIException    
     * 
     * 
     * @see MCRRestAPIObjects#listDerivates(UriInfo, String, String, String)
     */
    public static Response listDerivates(UriInfo info, String mcrObjID, String format,
        String sort) throws MCRRestAPIException {
        List<MCRRestAPIError> errors = new ArrayList<>();

        MCRRestAPISortObject sortObj = null;
        try {
            sortObj = createSortObject(sort);
        } catch (MCRRestAPIException rae) {
            errors.addAll(rae.getErrors());
        }

        //analyze format

        if (format.equals(MCRRestAPIObjects.FORMAT_JSON) || format.equals(MCRRestAPIObjects.FORMAT_XML)) {
            //ok
        } else {
            errors.add(new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_PARAMETER, "The Parameter format is wrong.",
                "Allowed values for format are 'json' or 'xml'."));
        }

        if (errors.size() > 0) {
            throw new MCRRestAPIException(Status.BAD_REQUEST, errors);
        }

        //Parameters are checked - continue to retrieve data

        List<MCRObjectIDDate> objIdDates = retrieveMCRObject(mcrObjID).getStructure().getDerivates().stream()
            .map(MCRMetaLinkID::getXLinkHrefID).filter(MCRMetadataManager::exists).map(id -> new MCRObjectIDDate() {
                long lastModified;
                {
                    try {
                        lastModified = MCRXMLMetadataManager.instance().getLastModified(id);
                    } catch (IOException e) {
                        lastModified = 0;
                        LOGGER.error(
                            "Exception while getting last modified of {}",
                            id, e);
                    }
                }

                @Override
                public String getId() {
                    return id.toString();
                }

                @Override
                public Date getLastModified() {
                    return new Date(lastModified);
                }
            }).sorted(new MCRRestAPISortObjectComparator(sortObj)::compare).collect(Collectors.toList());

        //output as XML
        if (MCRRestAPIObjects.FORMAT_XML.equals(format)) {
            Element eDerObjects = new Element("derobjects");
            Document docOut = new Document(eDerObjects);
            eDerObjects.setAttribute("numFound", Integer.toString(objIdDates.size()));
            for (MCRObjectIDDate oid : objIdDates) {
                Element eDerObject = new Element("derobject");
                eDerObject.setAttribute("ID", oid.getId());
                MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(oid.getId()));
                String mcrID = der.getDerivate().getMetaLink().getXLinkHref();
                eDerObject.setAttribute("metadata", mcrID);
                if (der.getLabel() != null) {
                    eDerObject.setAttribute("label", der.getLabel());
                }
                eDerObject.setAttribute("lastModified", SDF_UTC.format(oid.getLastModified()));
                eDerObject.setAttribute("href", info.getAbsolutePathBuilder().path(oid.getId()).build().toString());

                eDerObjects.addContent(eDerObject);
            }
            try {
                StringWriter sw = new StringWriter();
                XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
                xout.output(docOut, sw);
                return Response.ok(sw.toString())
                    .type("application/xml; charset=UTF-8")
                    .build();
            } catch (IOException e) {
                throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
                    new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR, GENERAL_ERROR_MSG, e.getMessage()));
            }
        }

        //output as JSON
        if (MCRRestAPIObjects.FORMAT_JSON.equals(format)) {
            StringWriter sw = new StringWriter();
            try {
                JsonWriter writer = new JsonWriter(sw);
                writer.setIndent("    ");
                writer.beginObject();
                writer.name("numFound").value(objIdDates.size());
                writer.name("mycoreobjects");
                writer.beginArray();
                for (MCRObjectIDDate oid : objIdDates) {
                    writer.beginObject();
                    writer.name("ID").value(oid.getId());
                    MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(oid.getId()));
                    String mcrID = der.getDerivate().getMetaLink().getXLinkHref();
                    writer.name("metadata").value(mcrID);
                    if (der.getLabel() != null) {
                        writer.name("label").value(der.getLabel());
                    }
                    writer.name("lastModified").value(SDF_UTC.format(oid.getLastModified()));
                    writer.name("href").value(info.getAbsolutePathBuilder().path(oid.getId()).build().toString());
                    writer.endObject();
                }
                writer.endArray();
                writer.endObject();

                writer.close();

                return Response.ok(sw.toString())
                    .type("application/json; charset=UTF-8")
                    .build();
            } catch (IOException e) {
                throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
                    new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR, GENERAL_ERROR_MSG, e.getMessage()));
            }
        }

        throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
            new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR, "Unexepected program flow termination.",
                "Please contact a developer!"));
    }

    /**
     * lists derivate content (file listing)
     * @param info - the Jersey UriInfo Object
     * @param request - the HTTPServletRequest object
     * @param mcrObjID - the MyCoRe Object ID
     * @param mcrDerID - the MyCoRe Derivate ID
     * @param format - the output format ('xml'|'json')
     * @param path - the sub path of a directory inside the derivate
     * @param depth - the level of subdirectories to be returned
     * @return a Jersey Response object
     * @throws MCRRestAPIException
     */
    public static Response listContents(UriInfo info, Request request, String mcrObjID,
        String mcrDerID, String format, String path, int depth) throws MCRRestAPIException {

        if (!format.equals(MCRRestAPIObjects.FORMAT_JSON) && !format.equals(MCRRestAPIObjects.FORMAT_XML)) {
            throw new MCRRestAPIException(Response.Status.BAD_REQUEST,
                new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_PARAMETER, "The syntax of format parameter is wrong.",
                    "Allowed values for format are 'json' or 'xml'."));
        }
        MCRObjectID mcrObj = MCRObjectID.getInstance(mcrObjID);
        MCRDerivate derObj = retrieveMCRDerivate(mcrObj, mcrDerID);

        try {
            MCRPath root = MCRPath.getPath(derObj.getId().toString(), "/");
            BasicFileAttributes readAttributes = Files.readAttributes(root, BasicFileAttributes.class);
            Date lastModified = new Date(readAttributes.lastModifiedTime().toMillis());
            ResponseBuilder responseBuilder = request.evaluatePreconditions(lastModified);
            if (responseBuilder != null) {
                return responseBuilder.build();
            }
            switch (format) {
                case MCRRestAPIObjects.FORMAT_XML:
                    Document docOut = listDerivateContentAsXML(derObj, path, depth, info);
                    try (StringWriter sw = new StringWriter()) {
                        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
                        xout.output(docOut, sw);
                        return response(sw.toString(), "application/xml", lastModified);
                    } catch (IOException e) {
                        throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
                            new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR, GENERAL_ERROR_MSG,
                                e.getMessage()));
                    }
                case MCRRestAPIObjects.FORMAT_JSON:
                    if (MCRRestAPIObjects.FORMAT_JSON.equals(format)) {
                        String result = listDerivateContentAsJson(derObj, path, depth, info);
                        return response(result, "application/json", lastModified);
                    }
                default:
                    throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
                        new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR,
                            "Unexepected program flow termination.",
                            "Please contact a developer!"));
            }
        } catch (IOException e) {
            throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR, new MCRRestAPIError(
                MCRRestAPIError.CODE_INTERNAL_ERROR, "Unexepected program flow termination.", e.getMessage()));
        }
    }

    /**
     * returns the URL of the main document of a derivate
     * 
     * @param info - the Jersey UriInfo object
     * @param mcrObjID - the MyCoRe Object ID
     * @param mcrDerID - the MyCoRe Derivate ID
     * 
     * @return the Resolving URL for the main document of the derivate
     * @throws IOException
     */
    public static String retrieveMaindocURL(UriInfo info, String mcrObjID, String mcrDerID) throws IOException {
        try {
            MCRObjectID mcrObj = MCRObjectID.getInstance(mcrObjID);
            MCRDerivate derObj = retrieveMCRDerivate(mcrObj, mcrDerID);
            String maindoc = derObj.getDerivate().getInternals().getMainDoc();

            String baseURL = MCRJerseyUtil.getBaseURL(info)
                + MCRConfiguration.instance().getString("MCR.RestAPI.v1.Files.URL.path");
            baseURL = baseURL.replace("${mcrid}", mcrObj.toString()).replace("${derid}",
                derObj.getId().toString());

            return baseURL + maindoc;
        } catch (MCRRestAPIException rae) {
            return null;
        }
    }

    private static Response response(String response, String type, Date lastModified) {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        String mimeType = type + "; charset=UTF-8";
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoTransform(false);
        cacheControl.setMaxAge(0);
        return Response.ok(responseBytes, mimeType).lastModified(lastModified)
            .header("Content-Length", responseBytes.length)
            .cacheControl(cacheControl).build();
    }

    /**
     * validates the given String if it matches the UTC syntax or the beginning of it
     * @param test
     * @return true, if it is valid
     */
    private static boolean validateDateInput(String test) {
        String base = "0000-00-00T00:00:00Z";
        if (test.length() > base.length())
            return false;
        test = test + base.substring(test.length());
        try {
            SDF_UTC.parse(test);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    private static MCRRestAPISortObject createSortObject(String input) throws MCRRestAPIException {
        if (input == null) {
            return null;
        }
        List<MCRRestAPIError> errors = new ArrayList<>();
        MCRRestAPISortObject result = new MCRRestAPISortObject();

        String[] data = input.split(":");
        if (data.length == 2) {
            result.setField(data[0].replace("|", ""));
            String sortOrder = data[1].toLowerCase(Locale.GERMAN).replace("|", "");
            if (!"ID".equals(result.getField()) && !"lastModified".equals(result.getField())) {
                errors.add(new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_QUERY_PARAMETER, "The sortField is wrong",
                    "Allowed values are 'ID' and 'lastModified'."));
            }

            if ("asc".equals(sortOrder)) {
                result.setOrder(SortOrder.ASC);
            }
            if ("desc".equals(sortOrder)) {
                result.setOrder(SortOrder.DESC);
            }
            if (result.getOrder() == null) {
                errors.add(new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_QUERY_PARAMETER, "The sortOrder is wrong",
                    "Allowed values for sortOrder are 'asc' and 'desc'."));
            }

        } else {
            errors.add(new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_QUERY_PARAMETER, "The sort parameter is wrong.",
                "The syntax should be [sortField]:[sortOrder]."));
        }
        if (errors.size() > 0) {
            throw new MCRRestAPIException(Status.BAD_REQUEST, errors);
        }
        return result;
    }

    private static MCRObject retrieveMCRObject(String idString) throws MCRRestAPIException {
        String key = "mcr"; // the default value for the key
        if (idString.contains(":")) {
            int pos = idString.indexOf(":");
            key = idString.substring(0, pos);
            idString = idString.substring(pos + 1);
            if (!key.equals("mcr")) {
                try {
                    idString = URLDecoder.decode(idString, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    //will not happen
                }
                //ToDo - Shall we restrict the key set with a property?

                //throw new MCRRestAPIException(MCRRestAPIError.create(Response.Status.BAD_REQUEST,
                //        "The ID is not valid.", "The prefix is unkown. Only 'mcr' is allowed."));
            }
        }
        if (key.equals("mcr")) {

            MCRObjectID mcrID = null;
            try {
                mcrID = MCRObjectID.getInstance(idString);
            } catch (Exception e) {
                throw new MCRRestAPIException(Response.Status.BAD_REQUEST,
                    new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_ID,
                        "The MyCoRe ID '" + idString
                            + "' is not valid. - Did you use the proper format: '{project}_{type}_{number}'?",
                        e.getMessage()));
            }

            if (!MCRMetadataManager.exists(mcrID)) {
                throw new MCRRestAPIException(Response.Status.NOT_FOUND,
                    new MCRRestAPIError(MCRRestAPIError.CODE_NOT_FOUND,
                        "There is no object with the given MyCoRe ID '" + idString + "'.", null));
            }

            return MCRMetadataManager.retrieveMCRObject(mcrID);
        } else {
            SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
            SolrQuery query = new SolrQuery();
            query.setQuery(key + ":" + idString);
            try {
                QueryResponse response = solrClient.query(query);
                SolrDocumentList solrResults = response.getResults();
                if (solrResults.getNumFound() == 1) {
                    String id = solrResults.get(0).getFieldValue("returnId").toString();
                    return retrieveMCRObject(id);
                } else {
                    if (solrResults.getNumFound() == 0) {
                        throw new MCRRestAPIException(Response.Status.NOT_FOUND,
                            new MCRRestAPIError(MCRRestAPIError.CODE_NOT_FOUND,
                                "There is no object with the given ID '" + key + ":" + idString + "'.", null));
                    } else {
                        throw new MCRRestAPIException(Response.Status.NOT_FOUND,
                            new MCRRestAPIError(MCRRestAPIError.CODE_NOT_FOUND,
                                "The ID is not unique. There are " + solrResults.getNumFound()
                                    + " objecst fore the given ID '" + key + ":" + idString + "'.",
                                null));
                    }
                }
            } catch (SolrServerException | IOException e) {
                LOGGER.error(e);
                throw new MCRRestAPIException(Response.Status.BAD_REQUEST,
                    new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR, "Internal server error.", e.getMessage()));
            }
        }
    }

    private static MCRDerivate retrieveMCRDerivate(MCRObjectID parentObjId, String derIDString)
        throws MCRRestAPIException {

        String derKey = "mcr"; // the default value for the key
        if (derIDString.contains(":")) {
            int pos = derIDString.indexOf(":");
            derKey = derIDString.substring(0, pos);
            derIDString = derIDString.substring(pos + 1);
            if (!derKey.equals("mcr") && !derKey.equals("label")) {
                throw new MCRRestAPIException(Response.Status.BAD_REQUEST,
                    new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_ID, "The ID is not valid.",
                        "The prefix is unkown. Only 'mcr' or 'label' are allowed."));
            }
        }

        String matchedDerID = null;
        if (derKey.equals("mcr")) {
            if (MCRMetadataManager.getDerivateIds(parentObjId, 0, TimeUnit.SECONDS)
                .stream()
                .map(MCRObjectID::toString)
                .anyMatch(derIDString::equals)) {
                matchedDerID = derIDString;
            }
        }
        if (derKey.equals("label")) {
            MCRObject mcrObj = MCRMetadataManager.retrieveMCRObject(parentObjId);
            for (MCRMetaLinkID check : mcrObj.getStructure().getDerivates()) {
                if (derIDString.equals(check.getXLinkLabel()) || derIDString.equals(check.getXLinkTitle())) {
                    matchedDerID = check.getXLinkHref();
                    break;
                }
            }
        }

        if (matchedDerID == null) {
            throw new MCRRestAPIException(Response.Status.NOT_FOUND,
                new MCRRestAPIError(MCRRestAPIError.CODE_NOT_FOUND, "Derivate " + derIDString + " not found.",
                    "The MyCoRe Object with id '" + parentObjId
                        + "' does not contain a derivate with id '" + derIDString + "'."));
        }

        MCRObjectID derID = MCRObjectID.getInstance(matchedDerID);
        if (!MCRMetadataManager.exists(derID)) {
            throw new MCRRestAPIException(Response.Status.NOT_FOUND, new MCRRestAPIError(MCRRestAPIError.CODE_NOT_FOUND,
                "There is no derivate with the id '" + matchedDerID + "'.", null));
        }
        return MCRMetadataManager.retrieveMCRDerivate(derID);
    }

    /**
     * checks if the given path is a directory and contains children
     * 
     * @param p - the path to check
     * @return true, if there are children
     */

    public static boolean hasChildren(Path p) {
        try {
            if (Files.isDirectory(p)) {
                DirectoryStream<Path> ds = Files.newDirectoryStream(p);
                return ds.iterator().hasNext();
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }
        return false;
    }
}
