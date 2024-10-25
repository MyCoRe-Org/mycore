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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.mycore.common.config.MCRConfiguration2;
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
import org.mycore.frontend.idmapper.MCRIDMapper;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.restapi.v1.MCRRestAPIObjects;
import org.mycore.restapi.v1.errors.MCRRestAPIError;
import org.mycore.restapi.v1.errors.MCRRestAPIException;
import org.mycore.restapi.v1.utils.MCRRestAPISortObject.SortOrder;

import com.google.gson.stream.JsonWriter;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

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
 */
public class MCRRestAPIObjectsHelper {
    private enum Mode {
        MCROBJECT, MCRDERIVATE
    };

    private static final String GENERAL_ERROR_MSG = "A problem occured while fetching the data.";

    private static Logger LOGGER = LogManager.getLogger(MCRRestAPIObjectsHelper.class);

    private static SimpleDateFormat SDF_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    private static MCRIDMapper ID_MAPPER = MCRConfiguration2
        .getInstanceOf(MCRIDMapper.class, MCRIDMapper.MCR_PROPERTY_CLASS).get();

    public static Response showMCRObject(String pathParamId, String queryParamStyle, UriInfo info, Application app)
        throws MCRRestAPIException {
        MCRObjectID mcrObjId = retrieveMCRObjectID(pathParamId);
        MCRObject mcrObj = MCRMetadataManager.retrieveMCRObject(mcrObjId);
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
                    Element currentDerElement = eDer;
                    try {
                        MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derID));
                        eDer.addContent(der.createXML().getRootElement().detach());

                        //<mycorederivate xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xlink="http://www.w3.org/1999/xlink" xsi:noNamespaceSchemaLocation="datamodel-derivate.xsd" ID="cpr_derivate_00003760" label="display_image" version="1.3">
                        //  <derivate display="true">

                        currentDerElement = eDer.getChild("mycorederivate").getChild("derivate");
                        Document docContents = listDerivateContentAsXML(
                            MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derID)), "/", -1, info, app);
                        if (docContents.hasRootElement()) {
                            eDer.addContent(docContents.getRootElement().detach());
                        }
                    } catch (MCRException e) {
                        currentDerElement.addContent(new Comment("Error: Derivate not found."));
                    } catch (IOException e) {
                        currentDerElement
                            .addContent(new Comment("Error: Derivate content could not be listed: " + e.getMessage()));
                    }
                }
            }
        }

        StringWriter sw = new StringWriter();
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        try {
            String filterId = MCRConfiguration2.getString("MCR.RestAPI.v1.Filter.XML").orElse("");
            if (filterId.length() > 0) {
                MCRContentTransformer trans = MCRContentTransformerFactory.getTransformer(filterId);
                Document filteredDoc = trans.transform(new MCRJDOMContent(doc)).asXML();
                outputter.output(filteredDoc, sw);
            } else {
                outputter.output(doc, sw);
            }
        } catch (JDOMException e) {
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

    public static Response showMCRDerivate(String paramMcrObjID, String paramMcrDerID, UriInfo info, Application app,
        boolean withDetails) throws MCRRestAPIException {
        try {
            MCRObjectID mcrObjId = retrieveMCRObjectID(paramMcrObjID);
            MCRObjectID derObjId = retrieveMCRDerivateID(mcrObjId, paramMcrDerID);
            MCRDerivate derObj = MCRMetadataManager.retrieveMCRDerivate(derObjId);

            Document doc = derObj.createXML();
            if (withDetails) {
                Document docContent = listDerivateContentAsXML(derObj, "/", -1, info, app);
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

    }

    private static String listDerivateContentAsJson(MCRDerivate derObj, String path, int depth, UriInfo info,
        Application app)
        throws IOException {
        StringWriter sw = new StringWriter();
        MCRPath root = MCRPath.getPath(derObj.getId().toString(), "/");
        root = MCRPath.toMCRPath(root.resolve(path));
        int finalDepth = (depth != -1) ? depth : Integer.MAX_VALUE;

        if (root != null) {
            JsonWriter writer = new JsonWriter(sw);
            Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), finalDepth,
                new MCRJSONFileVisitor(writer, derObj.getOwnerID(), derObj.getId(), info, app));
            writer.close();
        }
        return sw.toString();
    }

    private static Document listDerivateContentAsXML(MCRDerivate derObj, String path, int depth, UriInfo info,
        Application app)
        throws IOException {
        Document doc = new Document();

        MCRPath root = MCRPath.getPath(derObj.getId().toString(), "/");
        root = MCRPath.toMCRPath(root.resolve(path));
        int finalDepth = (depth != -1) ? depth : Integer.MAX_VALUE;

        if (root != null) {
            Element eContents = new Element("contents");
            eContents.setAttribute("mycoreobject", derObj.getOwnerID().toString());
            eContents.setAttribute("mycorederivate", derObj.getId().toString());
            doc.addContent(eContents);
            String finalPath = (path.endsWith("/")) ? path : path + "/";

            MCRPath p = MCRPath.getPath(derObj.getId().toString(), finalPath);
            if (p != null && Files.exists(p)) {
                Element eRoot = MCRPathXML.getDirectoryXML(p).getRootElement();
                eContents.addContent(eRoot.detach());
                createXMLForSubdirectories(p, eRoot, 1, finalDepth);
            }

            //add href Attributes
            String baseURL = MCRJerseyUtil.getBaseURL(info, app)
                + MCRConfiguration2.getStringOrThrow("MCR.RestAPI.v1.Files.URL.path");
            baseURL = baseURL.replace("${mcrid}", derObj.getOwnerID().toString()).replace("${derid}",
                derObj.getId().toString());
            XPathExpression<Element> xp = XPathFactory.instance().compile(".//child[@type='file']", Filters.element());
            for (Element e : xp.evaluate(eContents)) {
                String uri = e.getChildText("uri");
                if (uri != null) {
                    int pos = uri.lastIndexOf(":/");
                    String subPath = uri.substring(pos + 2);
                    while (subPath.startsWith("/")) {
                        subPath = subPath.substring(1);
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
     * @see MCRRestAPIObjects#listObjects(UriInfo, String, String, String)
     * 
     */
    public static Response listObjects(UriInfo info, String format, String filter, String sort)
        throws MCRRestAPIException {
        List<MCRRestAPIError> errors = new ArrayList<>();

        MCRRestAPISortObject sortObj = createSortObjectOrHandleError(sort, errors);
        validateFormat(format, errors);

        FilterParams filterParams = parseFilterParams(filter, errors);

        if (!errors.isEmpty()) {
            throw new MCRRestAPIException(Status.BAD_REQUEST, errors);
        }

        // Retrieve MCR IDs based on filter
        Set<String> mcrIDs = retrieveMCRIDs(filterParams);

        // Filter by modification dates
        List<MCRObjectIDDate> objIdDates = filterByModificationDates(mcrIDs, filterParams);

        if (sortObj != null) {
            objIdDates.sort(new MCRRestAPISortObjectComparator(sortObj));
        }
        return createListResponse(format, Mode.MCROBJECT, objIdDates, info);
    }

    private static FilterParams parseFilterParams(String filter, List<MCRRestAPIError> errors) {
        FilterParams params = new FilterParams();
        if (filter != null) {
            for (String s : filter.split(";")) {
                if (s.startsWith("project:")) {
                    params.projectIDs.add(s.substring(8));
                } else if (s.startsWith("type:")) {
                    params.typeIDs.add(s.substring(5));
                } else if (s.startsWith("lastModifiedBefore:")) {
                    params.lastModifiedBefore = validateDate(errors, s.substring(19), "lastModifiedBefore");
                } else if (s.startsWith("lastModifiedAfter:")) {
                    params.lastModifiedAfter = validateDate(errors, s.substring(18), "lastModifiedAfter");
                } else {
                    errors.add(new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_PARAMETER,
                        "The parameter 'filter' is wrong.", "Invalid filter syntax: " + s));
                }
            }
        }
        return params;
    }

    private static String validateDate(List<MCRRestAPIError> errors, String date, String field) {
        if (!validateDateInput(date)) {
            errors.add(new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_PARAMETER,
                "The parameter 'filter' is wrong.",
                "Invalid date format for " + field + ". Use UTC format: yyyy-MM-dd'T'HH:mm:ss'Z'."));
            return null;
        }
        return date;
    }

    private static Set<String> retrieveMCRIDs(FilterParams params) {
        Set<String> mcrIDs = new HashSet<>();
        if (params.projectIDs.isEmpty()) {
            if (params.typeIDs.isEmpty()) {
                mcrIDs = MCRXMLMetadataManager.instance().listIDs().stream().filter(id -> !id.contains("_derivate_"))
                    .collect(Collectors.toSet());
            } else {
                for (String type : params.typeIDs) {
                    mcrIDs.addAll(MCRXMLMetadataManager.instance().listIDsOfType(type));
                }
            }
        } else {
            for (String project : params.projectIDs) {
                for (String type : params.typeIDs) {
                    mcrIDs.addAll(MCRXMLMetadataManager.instance().listIDsForBase(project + "_" + type));
                }
            }
        }
        return mcrIDs;
    }

    private static List<MCRObjectIDDate> filterByModificationDates(Set<String> mcrIDs, FilterParams params)
        throws MCRRestAPIException {
        List<MCRObjectIDDate> objIdDates;
        try {
            objIdDates = MCRXMLMetadataManager.instance().retrieveObjectDates(new ArrayList<>(mcrIDs));
        } catch (IOException e) {
            throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
                new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR, GENERAL_ERROR_MSG, e.getMessage()));
        }

        if (params.lastModifiedBefore != null || params.lastModifiedAfter != null) {
            return objIdDates.stream().filter(oid -> {
                String lastModified = SDF_UTC.format(oid.getLastModified());
                if (params.lastModifiedAfter != null && lastModified.compareTo(params.lastModifiedAfter) < 0) {
                    return false;
                }
                return params.lastModifiedBefore == null || lastModified.compareTo(params.lastModifiedBefore) <= 0;
            }).collect(Collectors.toList());
        }

        return objIdDates;
    }

    private static Response createListResponse(String format, Mode mode, List<MCRObjectIDDate> objIdDates, UriInfo info)
        throws MCRRestAPIException {
        if (MCRRestAPIObjects.FORMAT_XML.equals(format)) {
            return createXmlListResponse(mode, info, objIdDates);
        } else if (MCRRestAPIObjects.FORMAT_JSON.equals(format)) {
            return createJsonListResponse(mode, info, objIdDates);

        }
        throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
            new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR, "A problem in programm flow", null));
    }

    /**
     * returns a list of derivate objects
     * @param info - the injected Jersey URIInfo object
     *
     * @param paramMcrObjID - the MyCoRe object id as string
     * @param format - the output format ('xml'|'json')
     * @param sort - the sort criteria
     *
     * @return a Jersey response object
     * @see MCRRestAPIObjects#listDerivates(UriInfo, String, String, String)
     */
    public static Response listDerivates(UriInfo info, String paramMcrObjID, String format, String sort)
        throws MCRRestAPIException {
        List<MCRRestAPIError> errors = new ArrayList<>();

        MCRRestAPISortObject sortObj = createSortObjectOrHandleError(sort, errors);
        validateFormat(format, errors);

        if (!errors.isEmpty()) {
            throw new MCRRestAPIException(Status.BAD_REQUEST, errors);
        }

        MCRObjectID mcrObjId = retrieveMCRObjectID(paramMcrObjID);
        List<MCRObjectIDDate> objIdDates = getDerivateObjectIDDates(mcrObjId, sortObj);

        return createListResponse(format, Mode.MCRDERIVATE, objIdDates, info);
    }

    protected static MCRRestAPISortObject createSortObjectOrHandleError(String sort, List<MCRRestAPIError> errors) {
        try {
            return createSortObject(sort);
        } catch (MCRRestAPIException e) {
            errors.addAll(e.getErrors());
            return null;
        }
    }

    private static void validateFormat(String format, List<MCRRestAPIError> errors) {
        if (!MCRRestAPIObjects.FORMAT_JSON.equals(format) && !MCRRestAPIObjects.FORMAT_XML.equals(format)) {
            errors.add(new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_PARAMETER, "The Parameter format is wrong.",
                "Allowed values for format are 'json' or 'xml'."));
        }
    }

    protected static List<MCRObjectIDDate> getDerivateObjectIDDates(MCRObjectID mcrObjId,
        MCRRestAPISortObject sortObj) {
        return MCRMetadataManager.retrieveMCRObject(mcrObjId)
            .getStructure().getDerivates().stream()
            .map(MCRMetaLinkID::getXLinkHrefID)
            .filter(MCRMetadataManager::exists)
            .map(id -> createMCRObjectIDDate(id))
            .sorted(new MCRRestAPISortObjectComparator(sortObj))
            .collect(Collectors.toList());
    }

    protected static MCRObjectIDDate createMCRObjectIDDate(MCRObjectID id) {
        return new MCRObjectIDDate() {
            long lastModified;

            {
                try {
                    lastModified = MCRXMLMetadataManager.instance().getLastModified(id);
                } catch (IOException e) {
                    lastModified = 0;
                    LOGGER.error("Exception while getting last modified of {}", id, e);
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
        };
    }

    protected static Response createXmlListResponse(Mode mode, UriInfo info, List<MCRObjectIDDate> objIdDates)
        throws MCRRestAPIException {
        Element elem = switch (mode) {
            case MCROBJECT -> new Element("mycoreobjects");
            case MCRDERIVATE -> new Element("derobjects");
        };
        elem.setAttribute("numFound", Integer.toString(objIdDates.size()));

        for (MCRObjectIDDate oid : objIdDates) {
            elem.addContent(createXmlListElement(mode, info, oid));
        }

        return buildXmlResponse(new Document(elem));
    }

    private static Element createXmlListElement(Mode mode, UriInfo info, MCRObjectIDDate oid) {
        Element elem = switch (mode) {
            case MCROBJECT -> new Element("mycoreobject");
            case MCRDERIVATE -> new Element("derobject");
        };

        elem.setAttribute("ID", oid.getId());
        elem.setAttribute("lastModified", SDF_UTC.format(oid.getLastModified()));
        elem.setAttribute("href", info.getAbsolutePathBuilder().path(oid.getId()).build().toString());

        if (mode == Mode.MCRDERIVATE) {
            MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(oid.getId()));
            elem.setAttribute("metadata", der.getDerivate().getMetaLink().getXLinkHref());

            if (!der.getDerivate().getClassifications().isEmpty()) {
                String classifications = der.getDerivate().getClassifications().stream()
                    .map(cl -> cl.getClassId() + ":" + cl.getCategId())
                    .collect(Collectors.joining(" "));
                elem.setAttribute("classifications", classifications);
            }
        }
        return elem;
    }

    private static Response buildXmlResponse(Document docOut) throws MCRRestAPIException {
        try (StringWriter sw = new StringWriter()) {
            new XMLOutputter(Format.getPrettyFormat()).output(docOut, sw);
            return Response.ok(sw.toString())
                .type("application/xml; charset=UTF-8")
                .build();
        } catch (IOException e) {
            throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
                new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR, GENERAL_ERROR_MSG, e.getMessage()));
        }
    }

    public static Response createJsonListResponse(Mode mode, UriInfo info, List<MCRObjectIDDate> objIdDates)
        throws MCRRestAPIException {
        try (StringWriter sw = new StringWriter(); JsonWriter writer = new JsonWriter(sw)) {
            writer.setIndent("    ");
            writer.beginObject();
            writer.name("numFound").value(objIdDates.size());
            switch (mode) {
                case MCROBJECT -> writer.name("mycoreobjects");
                case MCRDERIVATE -> writer.name("derobjects");
            }
            writer.beginArray();
            for (MCRObjectIDDate oid : objIdDates) {
                writer.beginObject();
                writer.name("ID").value(oid.getId());
                writer.name("lastModified").value(SDF_UTC.format(oid.getLastModified()));
                writer.name("href").value(info.getAbsolutePathBuilder().path(oid.getId()).build().toString());
                if (mode == Mode.MCRDERIVATE) {
                    MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(oid.getId()));
                    writer.name("metadata").value(der.getDerivate().getMetaLink().getXLinkHref());
                    if (!der.getDerivate().getClassifications().isEmpty()) {
                        List<String> classifications = der.getDerivate().getClassifications().stream()
                            .map(cl -> cl.getClassId() + ":" + cl.getCategId())
                            .collect(Collectors.toList());
                        writer.name("classifications").beginArray();
                        for (String c : classifications) {
                            writer.value(c);
                        }
                        writer.endArray();
                    }
                }
                writer.endObject();
            }

            writer.endArray();
            writer.endObject();
            return Response.ok(sw.toString())
                .type("application/json; charset=UTF-8")
                .build();
        } catch (IOException e) {
            throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
                new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR, GENERAL_ERROR_MSG, e.getMessage()));
        }
    }

    /**
     * lists derivate content (file listing)
     * @param info - the Jersey UriInfo Object
     * @param request - the HTTPServletRequest object
     * @param paramMcrObjID - the MyCoRe object id
     * @param paramMcrDerID - the MyCoRe derivate id
     * @param format - the output format ('xml'|'json')
     * @param path - the sub path of a directory inside the derivate
     * @param depth - the level of subdirectories to be returned
     * @return a Jersey Response object
     */
    public static Response listContents(UriInfo info, Application app, Request request, String paramMcrObjID,
        String paramMcrDerID, String format, String path, int depth) throws MCRRestAPIException {

        if (!format.equals(MCRRestAPIObjects.FORMAT_JSON) && !format.equals(MCRRestAPIObjects.FORMAT_XML)) {
            throw new MCRRestAPIException(Response.Status.BAD_REQUEST,
                new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_PARAMETER, "The syntax of format parameter is wrong.",
                    "Allowed values for format are 'json' or 'xml'."));
        }
        MCRObjectID mcrObjId = retrieveMCRObjectID(paramMcrObjID);
        MCRObjectID derObjId = retrieveMCRDerivateID(mcrObjId, paramMcrDerID);
        MCRDerivate derObj = MCRMetadataManager.retrieveMCRDerivate(derObjId);

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
                    Document docOut = listDerivateContentAsXML(derObj, path, depth, info, app);
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
                    String result = listDerivateContentAsJson(derObj, path, depth, info, app);
                    return response(result, "application/json", lastModified);
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
     * @param paramMcrObjID - the MyCoRe object id
     * @param paramMcrDerID - the MyCoRe derivate id
     *
     * @return the Resolving URL for the main document of the derivate
     */
    public static String retrieveMaindocURL(UriInfo info, String paramMcrObjID, String paramMcrDerID, Application app) {
        try {
            MCRObjectID mcrObjId = retrieveMCRObjectID(paramMcrObjID);
            MCRObjectID mcrDerId = retrieveMCRDerivateID(mcrObjId, paramMcrDerID);
            MCRDerivate derObj = MCRMetadataManager.retrieveMCRDerivate(mcrDerId);
            String maindoc = derObj.getDerivate().getInternals().getMainDoc();

            String baseURL = MCRJerseyUtil.getBaseURL(info, app)
                + MCRConfiguration2.getStringOrThrow("MCR.RestAPI.v1.Files.URL.path");
            baseURL = baseURL.replace("${mcrid}", mcrObjId.toString()).replace("${derid}", mcrDerId.toString());

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
     * @return true, if it is valid
     */
    private static boolean validateDateInput(String test) {
        String base = "0000-00-00T00:00:00Z";
        if (test.length() > base.length()) {
            return false;
        }
        try {
            SDF_UTC.parse(test + base.substring(test.length()));
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

            if (Objects.equals(sortOrder, "asc")) {
                result.setOrder(SortOrder.ASC);
            }
            if (Objects.equals(sortOrder, "desc")) {
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

    private static MCRObjectID retrieveMCRObjectID(String paramMcrObjId) throws MCRRestAPIException {
        Optional<MCRObjectID> optObjId = ID_MAPPER.mapMCRObjectID(paramMcrObjId);
        if (optObjId.isEmpty() || !MCRMetadataManager.exists(optObjId.get())) {
            throw new MCRRestAPIException(Response.Status.NOT_FOUND,
                new MCRRestAPIError(MCRRestAPIError.CODE_NOT_FOUND,
                    "There is no object with the given MyCoRe ID '" + paramMcrObjId + "'.", null));
        }
        return optObjId.get();
    }

    private static MCRObjectID retrieveMCRDerivateID(MCRObjectID parentObjId, String paramMcrDerId)
        throws MCRRestAPIException {
        return ID_MAPPER.mapMCRDerivateID(parentObjId, paramMcrDerId)
            .filter(MCRMetadataManager::exists)
            .orElseThrow(() -> new MCRRestAPIException(Response.Status.NOT_FOUND,
                new MCRRestAPIError(MCRRestAPIError.CODE_NOT_FOUND, "Derivate " + paramMcrDerId + " not found.",
                    "The MyCoRe Object with id '" + parentObjId
                        + "' does not contain a derivate with id '" + paramMcrDerId + "'.")));
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
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(p)) {
                    return ds.iterator().hasNext();
                }
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }
        return false;
    }

    // Helper class for filter parameters
    static class FilterParams {
        List<String> projectIDs = new ArrayList<>();
        List<String> typeIDs = new ArrayList<>();
        String lastModifiedBefore = null;
        String lastModifiedAfter = null;
    }
}
