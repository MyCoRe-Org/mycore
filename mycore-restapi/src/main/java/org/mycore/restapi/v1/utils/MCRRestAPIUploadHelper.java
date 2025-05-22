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
package org.mycore.restapi.v1.utils;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;
import static org.mycore.frontend.jersey.MCRJerseyUtil.APPLICATION_XML_UTF_8;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRExpandedObjectManager;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaEnrichedLinkID;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLangText;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;
import org.mycore.datamodel.niofs.utils.MCRTreeCopier;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.mycore.restapi.v1.errors.MCRRestAPIError;
import org.mycore.restapi.v1.errors.MCRRestAPIException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

public class MCRRestAPIUploadHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final java.nio.file.Path UPLOAD_DIR = Paths
        .get(MCRConfiguration2.getStringOrThrow("MCR.RestAPI.v1.Upload.Directory"));

    private static final String PATH_OBJECTS = "objects";

    private static final String PATH_DERIVATES = "derivates";

    private static final String PATH_CONTENTS = "contents";

    static {
        if (!Files.exists(UPLOAD_DIR)) {
            try {
                Files.createDirectories(UPLOAD_DIR);
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }
    }

    /**
     *
     * uploads a MyCoRe Object
     * based upon:
     * http://puspendu.wordpress.com/2012/08/23/restful-webservice-file-upload-with-jersey/
     *
     * @param info - the Jersey UriInfo object
     * @param request - the HTTPServletRequest object
     * @param uploadedInputStream - the inputstream from HTTP Post request
     * @param fileDetails - the file information from HTTP Post request
     * @return a Jersey Response object
     */
    public static Response uploadObject(UriInfo info, HttpServletRequest request, InputStream uploadedInputStream,
        FormDataContentDisposition fileDetails) throws MCRRestAPIException {

        java.nio.file.Path fXML = null;
        try {
            SAXBuilder sb = new SAXBuilder();
            Document docOut = sb.build(uploadedInputStream);

            MCRObjectID mcrID = MCRObjectID.getInstance(docOut.getRootElement().getAttributeValue("ID"));

            fXML = UPLOAD_DIR.resolve(mcrID.getBase() + '_' + UUID.randomUUID() + ".xml");
            XMLOutputter xmlOut = new XMLOutputter(Format.getPrettyFormat());
            try (BufferedWriter bw = Files.newBufferedWriter(fXML, StandardCharsets.UTF_8)) {
                xmlOut.output(docOut, bw);
            }

            MCRObject object = MCRObjectCommands.updateFromFile(fXML.toString(), false);// handles "create" as well
            mcrID = Objects.requireNonNull(object, "An error occurred while the object was created").getId();
            return Response.created(info.getBaseUriBuilder().path(PATH_OBJECTS + "/" + mcrID).build())
                .type(APPLICATION_XML_UTF_8)
                .build();
        } catch (Exception e) {
            LOGGER.error("Unable to Upload file: {}", fXML, e);
            MCRRestAPIException restAPIException = new MCRRestAPIException(Status.BAD_REQUEST,
                new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_PARAMETER,
                    "Unable to Upload file: " + fXML, e.getMessage()));
            restAPIException.initCause(e);
            throw restAPIException;
        } finally {
            if (fXML != null) {
                try {
                    Files.delete(fXML);
                } catch (IOException e) {
                    LOGGER.error("Unable to delete temporary workflow file: {}", fXML, e);
                }
            }
        }
    }

    /**
     * creates or updates a MyCoRe derivate
     * @param info - the Jersey UriInfo object
     * @param request - the HTTPServletRequest object
     * @param mcrObjID - the MyCoRe Object ID
     * @param label - the label of the new derivate
     * @param overwriteOnExisting, if true, an existing MyCoRe derivate
     *        with the given label or classification will be returned
     * @return a Jersey Response object
     */

    public static Response uploadDerivate(UriInfo info, HttpServletRequest request, String mcrObjID,
        String label, String classifications, boolean overwriteOnExisting) {
        MCRObjectID mcrObjIDObj = MCRObjectID.getInstance(mcrObjID);
        MCRObjectID derID = null;
        try {
            MCRObject mcrObj = MCRMetadataManager.retrieveMCRObject(mcrObjIDObj);
            if (overwriteOnExisting) {
                derID = findDerivateID(mcrObj, label, classifications);
            }
            if (derID == null) {
                derID = createNewDerivate(mcrObjIDObj, label, classifications);
            }
            return buildResponse(info, mcrObjIDObj, String.valueOf(derID));

        } catch (Exception e) {
            LOGGER.error("Exception while uploading derivate", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private static MCRObjectID findDerivateID(MCRObject mcrObj, String label, String classifications)
        throws MCRRestAPIException {
        MCRObjectID derID = null;
        final MCRCategoryDAO dao = MCRCategoryDAOFactory.obtainInstance();
        final List<MCRMetaEnrichedLinkID> currentDerivates =
            MCRExpandedObjectManager.getInstance().getExpandedObject(mcrObj).getStructure().getDerivates();
        if (label != null && !label.isEmpty()) {
            derID = findDerIDByLabel(currentDerivates, label);
        }
        if (derID == null && classifications != null && !classifications.isEmpty()) {
            derID = filterDerivateIDByClassifications(currentDerivates, classifications, dao);
        }
        return derID;
    }

    private static MCRObjectID findDerIDByLabel(List<MCRMetaEnrichedLinkID> derivates, String label) {
        for (MCRMetaLinkID derLink : derivates) {
            if (label.equals(derLink.getXLinkLabel()) || label.equals(derLink.getXLinkTitle())) {
                return derLink.getXLinkHrefID();

            }
        }
        return null;
    }

    private static MCRObjectID filterDerivateIDByClassifications(List<MCRMetaEnrichedLinkID> derivates,
        String classifications,
        MCRCategoryDAO dao) throws MCRRestAPIException {
        final List<MCRCategoryID> categories = Stream.of(classifications.split(" "))
            .map(MCRCategoryID::ofString)
            .toList();

        final List<MCRCategoryID> notExisting = categories.stream()
            .filter(Predicate.not(dao::exist))
            .toList();

        if (!notExisting.isEmpty()) {
            throw new MCRRestAPIException(Status.NOT_FOUND,
                new MCRRestAPIError(MCRRestAPIError.CODE_NOT_FOUND, "Classification not found.",
                    "There are no classifications with the IDs: " +
                        notExisting.stream().map(MCRCategoryID::toString).collect(Collectors.joining(", "))));
        }

        return derivates.stream()
            .filter(derLink -> {
                final Set<MCRCategoryID> clazzSet = new HashSet<>(derLink.getClassifications());
                return categories.stream().allMatch(clazzSet::contains);
            })
            .findFirst()
            .map(MCRMetaLinkID::getXLinkHrefID)
            .orElse(null);
    }

    private static MCRObjectID createNewDerivate(MCRObjectID mcrObjIDObj, String label, String classifications)
        throws MCRPersistenceException, MCRAccessException {
        MCRDerivate mcrDerivate = new MCRDerivate();

        MCRObjectDerivate derivate = mcrDerivate.getDerivate();
        if (label != null && !label.isEmpty()) {
            derivate.getTitles()
                .add(new MCRMetaLangText("title", null, null, 0, null, label));
        }

        MCRObjectID zeroDerId =
            MCRObjectID.getInstance(MCRObjectID.formatID(mcrObjIDObj.getProjectId() + "_derivate", 0));
        mcrDerivate.setId(zeroDerId);
        mcrDerivate.setSchema("datamodel-derivate.xsd");
        derivate.setLinkMeta(new MCRMetaLinkID(MCRObjectDerivate.ELEMENT_LINKMETA, mcrObjIDObj, null, null));
        derivate.setInternals(new MCRMetaIFS(MCRObjectDerivate.ELEMENT_INTERNAL, null));
        if (classifications != null && !classifications.isEmpty()) {
            addClassificationsToDerivate(mcrDerivate, classifications);
        }
        MCRMetadataManager.create(mcrDerivate);
        return mcrDerivate.getId();
    }

    private static void addClassificationsToDerivate(MCRDerivate mcrDerivate, String classifications) {
        final MCRCategoryDAO dao = MCRCategoryDAOFactory.obtainInstance();
        final List<MCRMetaClassification> currentClassifications = mcrDerivate.getDerivate().getClassifications();

        Stream.of(classifications.split(" "))
            .map(MCRCategoryID::ofString)
            .filter(dao::exist)
            .map(categoryID -> new MCRMetaClassification("classification", 0, null, categoryID))
            .forEach(currentClassifications::add);
    }

    /**
     * uploads a file into a given derivate
     *
     * @param info                - the Jersey UriInfo object
     * @param pathParamMcrObjID   - a MyCoRe Object ID
     * @param pathParamMcrDerID   - a MyCoRe Derivate ID
     * @param uploadedInputStream - the inputstream from HTTP Post request
     * @param formParamPath       - the path of the file inside the derivate
     * @param formParamMaindoc    - true, if this file should be marked as maindoc
     * @param formParamUnzip      - true, if the upload is zip file that should be unzipped inside the derivate
     * @param formParamMD5        - the MD5 sum of the uploaded file
     * @param formParamSize       - the size of the uploaded file
     * @return a Jersey Response object
     */

    public static Response uploadFile(UriInfo info, String pathParamMcrObjID,
        String pathParamMcrDerID, InputStream uploadedInputStream,
        String formParamPath, boolean formParamMaindoc, boolean formParamUnzip,
        String formParamMD5, Long formParamSize) throws MCRRestAPIException {
        createParameterMap(pathParamMcrObjID, pathParamMcrDerID, formParamPath,
            formParamMaindoc, formParamUnzip, formParamMD5, formParamSize);

        MCRObjectID objID = MCRObjectID.getInstance(pathParamMcrObjID);
        MCRObjectID derID = MCRObjectID.getInstance(pathParamMcrDerID);
        MCRDerivate der = retrieveAndValidateDerivate(objID, derID);

        try {
            handleFileUpload(uploadedInputStream, formParamPath, formParamMaindoc, formParamUnzip, derID, der);
        } catch (IOException | MCRPersistenceException | MCRAccessException e) {
            LOGGER.error(e);
            MCRRestAPIException restAPIException = new MCRRestAPIException(Status.INTERNAL_SERVER_ERROR,
                new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR, "Internal error", e.getMessage()));
            restAPIException.initCause(e);
            throw restAPIException;
        }

        return buildResponse(info, objID, derID + "/" + PATH_CONTENTS);
    }

    private static SortedMap<String, String> createParameterMap(String pathParamMcrObjID, String pathParamMcrDerID,
        String formParamPath, boolean formParamMaindoc,
        boolean formParamUnzip, String formParamMD5,
        Long formParamSize) {
        SortedMap<String, String> parameter = new TreeMap<>();
        parameter.put("mcrObjectID", pathParamMcrObjID);
        parameter.put("mcrDerivateID", pathParamMcrDerID);
        parameter.put("path", formParamPath);
        parameter.put("maindoc", Boolean.toString(formParamMaindoc));
        parameter.put("unzip", Boolean.toString(formParamUnzip));
        parameter.put("md5", formParamMD5);
        parameter.put("size", Long.toString(formParamSize));
        return parameter;
    }

    private static MCRDerivate retrieveAndValidateDerivate(MCRObjectID objID, MCRObjectID derID)
        throws MCRRestAPIException {
        if (!MCRAccessManager.checkPermission(derID.toString(), PERMISSION_WRITE)) {
            throw new MCRRestAPIException(Status.FORBIDDEN,
                new MCRRestAPIError(MCRRestAPIError.CODE_ACCESS_DENIED, "Could not add file to derivate",
                    "You do not have the permission to write to " + derID));
        }

        MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(derID);

        if (!der.getOwnerID().equals(objID)) {
            throw new MCRRestAPIException(Status.INTERNAL_SERVER_ERROR,
                new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR, "Derivate object mismatch",
                    "Derivate " + derID + " belongs to a different object: " + objID));
        }

        return der;
    }

    private static void handleFileUpload(InputStream uploadedInputStream, String formParamPath,
        boolean formParamMaindoc,
        boolean formParamUnzip, MCRObjectID derID, MCRDerivate der)
        throws IOException, MCRPersistenceException, MCRAccessException {
        java.nio.file.Path derDir = UPLOAD_DIR.resolve(derID.toString());
        MCRPath derRoot = MCRPath.getPath(derID.toString(), "/");

        cleanUploadDirectory(derDir);

        String sanitizedPath = sanitizePath(formParamPath);

        if (formParamUnzip) {
            unzipAndProcessFiles(uploadedInputStream, derDir, derRoot, formParamMaindoc, der);
        } else {
            saveSingleFile(uploadedInputStream, derDir, sanitizedPath, derRoot, formParamMaindoc, der);
        }

        MCRMetadataManager.update(der);
        cleanUploadDirectory(derDir);
    }

    private static void cleanUploadDirectory(java.nio.file.Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walkFileTree(dir, new MCRRecursiveDeleter());
        }
    }

    private static String sanitizePath(String path) {
        String newPath = path.replace("\\", "/").replace("../", "");
        while (newPath.startsWith("/")) {
            newPath = newPath.substring(1);
        }
        return newPath;
    }

    private static void unzipAndProcessFiles(InputStream uploadedInputStream, java.nio.file.Path derDir,
        MCRPath derRoot,
        boolean formParamMaindoc, MCRDerivate der) throws IOException {
        String maindoc = null;
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(uploadedInputStream))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                LOGGER.debug("Unzipping: {}", entry::getName);
                java.nio.file.Path target = MCRUtils.safeResolve(derDir, entry.getName());
                Files.createDirectories(target.getParent());
                Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                if (maindoc == null && !entry.isDirectory()) {
                    maindoc = entry.getName();
                }
                entry = zis.getNextEntry();
            }
        }
        Files.walkFileTree(derDir, new MCRTreeCopier(derDir, derRoot, true));
        if (formParamMaindoc) {
            der.getDerivate().getInternals().setMainDoc(maindoc);
        }
    }

    private static void saveSingleFile(InputStream uploadedInputStream, java.nio.file.Path derDir, String path,
        MCRPath derRoot,
        boolean formParamMaindoc, MCRDerivate der) throws IOException {
        java.nio.file.Path saveFile = MCRUtils.safeResolve(derDir, path);
        Files.createDirectories(saveFile.getParent());
        Files.copy(uploadedInputStream, saveFile, StandardCopyOption.REPLACE_EXISTING);
        Files.walkFileTree(derDir, new MCRTreeCopier(derDir, derRoot, true));
        if (formParamMaindoc) {
            der.getDerivate().getInternals().setMainDoc(path);
        }
    }

    private static Response buildResponse(UriInfo info, MCRObjectID objID, String derID) {
        UriBuilder uriBuilder = info.getBaseUriBuilder();
        return Response
            .created(uriBuilder.path(PATH_OBJECTS + "/" + objID + "/" + PATH_DERIVATES + "/" + derID).build())
            .type(APPLICATION_XML_UTF_8).build();
    }

    /**
     * deletes all files inside a given derivate
     * @param info - the Jersey UriInfo object
     * @param request - the HTTPServletRequest object
     * @param pathParamMcrObjID - the MyCoRe Object ID
     * @param pathParamMcrDerID - the MyCoRe Derivate ID
     * @return a Jersey Response Object
     */
    public static Response deleteAllFiles(UriInfo info, HttpServletRequest request, String pathParamMcrObjID,
        String pathParamMcrDerID) {

        MCRObjectID objID = MCRObjectID.getInstance(pathParamMcrObjID);
        MCRObjectID derID = MCRObjectID.getInstance(pathParamMcrDerID);

        //MCRAccessManager.checkPermission uses CACHE, which seems to be dirty from other calls
        MCRAccessManager.invalidPermissionCache(derID.toString(), PERMISSION_WRITE);
        if (MCRAccessManager.checkPermission(derID.toString(), PERMISSION_WRITE)) {
            MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(derID);

            final MCRPath rootPath = MCRPath.getPath(der.getId().toString(), "/");
            try {
                Files.walkFileTree(rootPath, new MCRRecursiveDeleter());
                Files.createDirectory(rootPath);
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }

        return Response
            .created(info.getBaseUriBuilder()
                .path(PATH_OBJECTS + "/" + objID + "/" + PATH_DERIVATES + "/" + derID + "/" + PATH_CONTENTS)
                .build())
            .type(APPLICATION_XML_UTF_8)
            .build();
    }

    /**
     * deletes a whole derivate
     * @param info - the Jersey UriInfo object
     * @param request - the HTTPServletRequest object
     * @param pathParamMcrObjID - the MyCoRe Object ID
     * @param pathParamMcrDerID - the MyCoRe Derivate ID
     * @return a Jersey Response Object
     */
    public static Response deleteDerivate(UriInfo info, HttpServletRequest request, String pathParamMcrObjID,
        String pathParamMcrDerID) throws MCRRestAPIException {

        MCRObjectID objID = MCRObjectID.getInstance(pathParamMcrObjID);
        MCRObjectID derID = MCRObjectID.getInstance(pathParamMcrDerID);

        try {
            MCRMetadataManager.deleteMCRDerivate(derID);
            return Response
                .created(info.getBaseUriBuilder().path(PATH_OBJECTS + "/" + objID + "/" + PATH_DERIVATES).build())
                .type(APPLICATION_XML_UTF_8)
                .build();
        } catch (MCRAccessException e) {
            MCRRestAPIException restAPIException = new MCRRestAPIException(Status.FORBIDDEN,
                new MCRRestAPIError(MCRRestAPIError.CODE_ACCESS_DENIED, "Could not delete derivate", e.getMessage()));
            restAPIException.initCause(e);
            throw restAPIException;
        }
    }

    /**
     * serializes a map of Strings into a compact JSON structure
     * @param data a sorted Map of Strings
     * @return a compact JSON
     */
    public static String generateMessagesFromProperties(SortedMap<String, String> data) {
        StringWriter sw = new StringWriter();
        sw.append('{');
        for (String key : data.keySet()) {
            sw.append("\"").append(key).append("\"").append(':').append("\"").append(data.get(key)).append("\"")
                .append(',');
        }
        String result = sw.toString();
        if (result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        result = result + "}";

        return result;
    }

}
