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

import static org.mycore.access.MCRAccessManager.PERMISSION_DELETE;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Signature;
import java.util.Base64;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFileImportExport;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.restapi.v1.errors.MCRRestAPIError;
import org.mycore.restapi.v1.errors.MCRRestAPIException;
import org.mycore.user2.MCRUserManager;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;

public class MCRRestAPIUploadHelper {
    private static final Logger LOGGER = LogManager.getLogger(MCRRestAPIUploadHelper.class);

    private static final String HEADER_NAME_AUTHORIZATION = "Authorization";

    public static final String FORMAT_XML = "xml";

    private static java.nio.file.Path UPLOAD_DIR = Paths
        .get(MCRConfiguration.instance().getString("MCR.RestAPI.v1.Upload.Directory"));
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
     * @throws MCRRestAPIException
     */
    public static Response uploadObject(UriInfo info, HttpServletRequest request, InputStream uploadedInputStream,
        FormDataContentDisposition fileDetails) throws MCRRestAPIException {

        SignedJWT signedJWT = MCRJSONWebTokenUtil.retrieveAuthenticationToken(request);
        java.nio.file.Path fXML = null;
        try (MCRJPATransactionWrapper mtw = new MCRJPATransactionWrapper()) {
            SAXBuilder sb = new SAXBuilder();
            Document docOut = sb.build(uploadedInputStream);

            MCRObjectID mcrID = MCRObjectID.getInstance(docOut.getRootElement().getAttributeValue("ID"));
            if (mcrID.getNumberAsInteger() == 0) {
                mcrID = MCRObjectID.getNextFreeId(mcrID.getBase());
            }

            fXML = UPLOAD_DIR.resolve(mcrID + ".xml");

            docOut.getRootElement().setAttribute("ID", mcrID.toString());
            docOut.getRootElement().setAttribute("label", mcrID.toString());
            XMLOutputter xmlOut = new XMLOutputter(Format.getPrettyFormat());
            try (BufferedWriter bw = Files.newBufferedWriter(fXML, StandardCharsets.UTF_8)) {
                xmlOut.output(docOut, bw);
            }

            MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
            MCRUserInformation currentUser = mcrSession.getUserInformation();
            MCRUserInformation apiUser = MCRUserManager
                .getUser(MCRJSONWebTokenUtil.retrieveUsernameFromAuthenticationToken(signedJWT));
            mcrSession.setUserInformation(apiUser);
            MCRObjectCommands.updateFromFile(fXML.toString(), false); // handles "create" as well
            mcrSession.setUserInformation(currentUser);

            return Response.created(info.getBaseUriBuilder().path("v1/objects/" + mcrID).build())
                .type("application/xml; charset=UTF-8")
                .header(HEADER_NAME_AUTHORIZATION, MCRJSONWebTokenUtil.createJWTAuthorizationHeader(signedJWT)).build();

        } catch (Exception e) {
            LOGGER.error("Unable to Upload file: {}", String.valueOf(fXML), e);
            throw new MCRRestAPIException(Status.BAD_REQUEST, new MCRRestAPIError(MCRRestAPIError.CODE_WRONG_PARAMETER,
                "Unable to Upload file: " + String.valueOf(fXML), e.getMessage()));
        } finally {
            if (fXML != null) {
                try {
                    Files.delete(fXML);
                } catch (IOException e) {
                    LOGGER.error("Unable to delete temporary workflow file: {}", String.valueOf(fXML), e);
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
     * @param overwriteOnExistingLabel, if true an existing MyCoRe derivate with the given label will be returned 
     * @return a Jersey Response object
     * @throws MCRRestAPIException
     */
    public static Response uploadDerivate(UriInfo info, HttpServletRequest request, String mcrObjID, String label,
        boolean overwriteOnExistingLabel) throws MCRRestAPIException {
        Response response = Response.status(Status.INTERNAL_SERVER_ERROR).build();

        SignedJWT signedJWT = MCRJSONWebTokenUtil.retrieveAuthenticationToken(request);
        //  File fXML = null;
        MCRObjectID mcrObjIDObj = MCRObjectID.getInstance(mcrObjID);

        try (MCRJPATransactionWrapper mtw = new MCRJPATransactionWrapper()) {
            MCRSession session = MCRServlet.getSession(request);
            MCRUserInformation currentUser = session.getUserInformation();
            MCRUserInformation apiUser = MCRUserManager
                .getUser(MCRJSONWebTokenUtil.retrieveUsernameFromAuthenticationToken(signedJWT));
            session.setUserInformation(apiUser);

            MCRObject mcrObj = MCRMetadataManager.retrieveMCRObject(mcrObjIDObj);
            MCRObjectID derID = null;
            if (overwriteOnExistingLabel) {
                for (MCRMetaLinkID derLink : mcrObj.getStructure().getDerivates()) {
                    if (label.equals(derLink.getXLinkLabel()) || label.equals(derLink.getXLinkTitle())) {
                        derID = derLink.getXLinkHrefID();
                    }
                }
            }

            if (derID == null) {
                derID = MCRObjectID.getNextFreeId(mcrObjIDObj.getProjectId() + "_derivate");
                MCRDerivate mcrDerivate = new MCRDerivate();
                mcrDerivate.setLabel(label);
                mcrDerivate.setId(derID);
                mcrDerivate.setSchema("datamodel-derivate.xsd");
                mcrDerivate.getDerivate().setLinkMeta(new MCRMetaLinkID("linkmeta", mcrObjIDObj, null, null));
                mcrDerivate.getDerivate()
                    .setInternals(new MCRMetaIFS("internal", UPLOAD_DIR.resolve(derID.toString()).toString()));

                MCRMetadataManager.create(mcrDerivate);
                MCRMetadataManager.addOrUpdateDerivateToObject(mcrObjIDObj,
                    new MCRMetaLinkID("derobject", derID, null, label));
            }

            response = Response
                .created(info.getBaseUriBuilder().path("v1/objects/" + mcrObjID + "/derivates/" + derID).build())
                .type("application/xml; charset=UTF-8")
                .header(HEADER_NAME_AUTHORIZATION, MCRJSONWebTokenUtil.createJWTAuthorizationHeader(signedJWT)).build();
            session.setUserInformation(currentUser);
        } catch (Exception e) {
            LOGGER.error("Exeption while uploading derivate", e);
        }
        return response;
    }

    /**
     * uploads a file into a given derivate
     * @param info - the Jersey UriInfo object
     * @param request - the HTTPServletRequest object 
     * @param pathParamMcrObjID - a MyCoRe Object ID
     * @param pathParamMcrDerID - a MyCoRe Derivate ID
     * @param uploadedInputStream - the inputstream from HTTP Post request
     * @param fileDetails - the file information from HTTP Post request
     * @param formParamPath - the path of the file inside the derivate
     * @param formParamMaindoc - true, if this file should be marked as maindoc
     * @param formParamUnzip - true, if the upload is zip file that should be unzipped inside the derivate
     * @param formParamMD5 - the MD5 sum of the uploaded file 
     * @param formParamSize - the size of the uploaded file
     * @return a Jersey Response object
     * @throws MCRRestAPIException
     */
    public static Response uploadFile(UriInfo info, HttpServletRequest request, String pathParamMcrObjID,
        String pathParamMcrDerID, InputStream uploadedInputStream, FormDataContentDisposition fileDetails,
        String formParamPath, boolean formParamMaindoc, boolean formParamUnzip, String formParamMD5, Long formParamSize)
        throws MCRRestAPIException {

        SignedJWT signedJWT = MCRJSONWebTokenUtil.retrieveAuthenticationToken(request);
        SortedMap<String, String> parameter = new TreeMap<>();
        parameter.put("mcrObjectID", pathParamMcrObjID);
        parameter.put("mcrDerivateID", pathParamMcrDerID);
        parameter.put("path", formParamPath);
        parameter.put("maindoc", Boolean.toString(formParamMaindoc));
        parameter.put("unzip", Boolean.toString(formParamUnzip));
        parameter.put("md5", formParamMD5);
        parameter.put("size", Long.toString(formParamSize));

        String base64Signature = request.getHeader("X-MyCoRe-RestAPI-Signature");
        if (base64Signature == null) {
            throw new MCRRestAPIException(Status.UNAUTHORIZED,
                new MCRRestAPIError(MCRRestAPIError.CODE_INVALID_AUTHENCATION,
                    "The submitted data could not be validated.",
                    "Please provide a signature as HTTP header 'X-MyCoRe-RestAPI-Signature'."));
        }
        if (verifyPropertiesWithSignature(parameter, base64Signature,
            MCRJSONWebTokenUtil.retrievePublicKeyFromAuthenticationToken(signedJWT))) {
            try (MCRJPATransactionWrapper mtw = new MCRJPATransactionWrapper()) {
                //MCRSession session = MCRServlet.getSession(request);
                MCRSession session = MCRSessionMgr.getCurrentSession();
                MCRUserInformation currentUser = session.getUserInformation();

                MCRUserInformation apiUser = MCRUserManager
                    .getUser(MCRJSONWebTokenUtil.retrieveUsernameFromAuthenticationToken(signedJWT));
                session.setUserInformation(apiUser);
                MCRObjectID objID = MCRObjectID.getInstance(pathParamMcrObjID);
                MCRObjectID derID = MCRObjectID.getInstance(pathParamMcrDerID);

                MCRAccessManager.invalidPermissionCache(derID.toString(), PERMISSION_WRITE);
                if (MCRAccessManager.checkPermission(derID.toString(), PERMISSION_WRITE)) {

                    MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(derID);

                    java.nio.file.Path derDir = null;

                    String path = null;
                    if (der.getOwnerID().equals(objID)) {
                        try {
                            derDir = UPLOAD_DIR.resolve(derID.toString());
                            if (Files.exists(derDir)) {
                                Files.walkFileTree(derDir, MCRRecursiveDeleter.instance());
                            }
                            path = formParamPath.replace("\\", "/").replace("../", "");
                            while (path.startsWith("/")) {
                                path = path.substring(1);
                            }

                            MCRDirectory difs = MCRDirectory.getRootDirectory(derID.toString());
                            if (difs == null) {
                                difs = new MCRDirectory(derID.toString());
                            }

                            der.getDerivate().getInternals().setIFSID(difs.getID());
                            der.getDerivate().getInternals().setSourcePath(derDir.toString());

                            if (formParamUnzip) {
                                String maindoc = null;
                                try (ZipInputStream zis = new ZipInputStream(
                                    new BufferedInputStream(uploadedInputStream))) {
                                    ZipEntry entry;
                                    while ((entry = zis.getNextEntry()) != null) {
                                        LOGGER.debug("Unzipping: {}", entry.getName());
                                        java.nio.file.Path target = derDir.resolve(entry.getName());
                                        Files.createDirectories(target.getParent());
                                        Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                                        if (maindoc == null && !entry.isDirectory()) {
                                            maindoc = entry.getName();
                                        }
                                    }
                                } catch (IOException e) {
                                    LOGGER.error(e);
                                }

                                MCRFileImportExport.importFiles(derDir.toFile(), difs);

                                if (formParamMaindoc) {
                                    der.getDerivate().getInternals().setMainDoc(maindoc);
                                }
                            } else {
                                java.nio.file.Path saveFile = derDir.resolve(path);
                                Files.createDirectories(saveFile.getParent());
                                Files.copy(uploadedInputStream, saveFile, StandardCopyOption.REPLACE_EXISTING);
                                //delete old file
                                MCRFileImportExport.importFiles(derDir.toFile(), difs);
                                if (formParamMaindoc) {
                                    der.getDerivate().getInternals().setMainDoc(path);
                                }
                            }

                            MCRMetadataManager.update(der);
                            Files.walkFileTree(derDir, MCRRecursiveDeleter.instance());
                        } catch (IOException | MCRPersistenceException | MCRAccessException e) {
                            LOGGER.error(e);
                            throw new MCRRestAPIException(Status.INTERNAL_SERVER_ERROR, new MCRRestAPIError(
                                MCRRestAPIError.CODE_INTERNAL_ERROR, "Internal error", e.getMessage()));
                        }
                    }
                    session.setUserInformation(currentUser);
                    return Response
                        .created(info.getBaseUriBuilder()
                            .path("v1/objects/" + objID + "/derivates/" + derID + "/contents")
                            .build())
                        .type("application/xml; charset=UTF-8")
                        .header(HEADER_NAME_AUTHORIZATION, MCRJSONWebTokenUtil.createJWTAuthorizationHeader(signedJWT))
                        .build();
                }
            }
        }
        throw new MCRRestAPIException(Status.FORBIDDEN, new MCRRestAPIError(MCRRestAPIError.CODE_INVALID_DATA,
            "File upload failed.", "The submitted data could not be validated."));
    }

    /**
     * deletes all files inside a given derivate
     * @param info - the Jersey UriInfo object
     * @param request - the HTTPServletRequest object 
     * @param pathParamMcrObjID - the MyCoRe Object ID
     * @param pathParamMcrDerID - the MyCoRe Derivate ID
     * @return a Jersey Response Object
     * @throws MCRRestAPIException
     */
    public static Response deleteAllFiles(UriInfo info, HttpServletRequest request, String pathParamMcrObjID,
        String pathParamMcrDerID) throws MCRRestAPIException {

        Response response = Response.status(Status.INTERNAL_SERVER_ERROR).build();

        SignedJWT signedJWT = MCRJSONWebTokenUtil.retrieveAuthenticationToken(request);
        SortedMap<String, String> parameter = new TreeMap<>();
        parameter.put("mcrObjectID", pathParamMcrObjID);
        parameter.put("mcrDerivateID", pathParamMcrDerID);

        String base64Signature = request.getHeader("X-MyCoRe-RestAPI-Signature");
        if (base64Signature == null) {
            //ToDo error handling
        }
        if (verifyPropertiesWithSignature(parameter, base64Signature,
            MCRJSONWebTokenUtil.retrievePublicKeyFromAuthenticationToken(signedJWT))) {
            try (MCRJPATransactionWrapper mtw = new MCRJPATransactionWrapper()) {
                //MCRSession session = MCRServlet.getSession(request);
                MCRSession session = MCRSessionMgr.getCurrentSession();
                MCRUserInformation currentUser = session.getUserInformation();
                MCRUserInformation apiUser = MCRUserManager
                    .getUser(MCRJSONWebTokenUtil.retrieveUsernameFromAuthenticationToken(signedJWT));
                session.setUserInformation(apiUser);
                MCRObjectID objID = MCRObjectID.getInstance(pathParamMcrObjID);
                MCRObjectID derID = MCRObjectID.getInstance(pathParamMcrDerID);

                //MCRAccessManager.checkPermission uses CACHE, which seems to be dirty from other calls
                MCRAccessManager.invalidPermissionCache(derID.toString(), PERMISSION_WRITE);
                if (MCRAccessManager.checkPermission(derID.toString(), PERMISSION_WRITE)) {
                    MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(derID);

                    final MCRPath rootPath = MCRPath.getPath(der.getId().toString(), "/");
                    try {
                        Files.walkFileTree(rootPath, MCRRecursiveDeleter.instance());
                        Files.createDirectory(rootPath);
                    } catch (IOException e) {
                        LOGGER.error(e);
                    }
                }

                session.setUserInformation(currentUser);
                response = Response
                    .created(info.getBaseUriBuilder()
                        .path("v1/objects/" + objID + "/derivates/" + derID + "/contents")
                        .build())
                    .type("application/xml; charset=UTF-8")
                    .header(HEADER_NAME_AUTHORIZATION, MCRJSONWebTokenUtil.createJWTAuthorizationHeader(signedJWT))
                    .build();
            }
        } else {
            throw new MCRRestAPIException(Status.FORBIDDEN, new MCRRestAPIError(MCRRestAPIError.CODE_INVALID_DATA,
                "Delete failed.", "The submitted data could not be validated."));
        }
        return response;
    }

    /**
     * deletes a whole derivate
     * @param info - the Jersey UriInfo object
     * @param request - the HTTPServletRequest object 
     * @param pathParamMcrObjID - the MyCoRe Object ID
     * @param pathParamMcrDerID - the MyCoRe Derivate ID
     * @return a Jersey Response Object
     * @throws MCRRestAPIException
     */
    public static Response deleteDerivate(UriInfo info, HttpServletRequest request, String pathParamMcrObjID,
        String pathParamMcrDerID) throws MCRRestAPIException {

        Response response = Response.status(Status.INTERNAL_SERVER_ERROR).build();

        SignedJWT signedJWT = MCRJSONWebTokenUtil.retrieveAuthenticationToken(request);

        String base64Signature = request.getHeader("X-MyCoRe-RestAPI-Signature");
        if (base64Signature == null) {
            //ToDo error handling
        }
        try (MCRJPATransactionWrapper mtw = new MCRJPATransactionWrapper()) {
            //MCRSession session = MCRServlet.getSession(request);
            MCRSession session = MCRSessionMgr.getCurrentSession();
            MCRUserInformation currentUser = session.getUserInformation();
            session.setUserInformation(
                MCRUserManager.getUser(MCRJSONWebTokenUtil.retrieveUsernameFromAuthenticationToken(signedJWT)));
            MCRObjectID objID = MCRObjectID.getInstance(pathParamMcrObjID);
            MCRObjectID derID = MCRObjectID.getInstance(pathParamMcrDerID);

            //MCRAccessManager.checkPermission() uses CACHE, which seems to be dirty from other calls????
            MCRAccessManager.invalidPermissionCache(derID.toString(), PERMISSION_DELETE);
            if (MCRAccessManager.checkPermission(derID.toString(), PERMISSION_DELETE)) {
                try {
                    MCRMetadataManager.deleteMCRDerivate(derID);
                } catch (MCRPersistenceException pe) {
                    //dir does not exist - do nothing
                } catch (MCRAccessException e) {
                    LOGGER.error(e);
                }
            }
            session.setUserInformation(currentUser);
            response = Response
                .created(info.getBaseUriBuilder().path("v1/objects/" + objID + "/derivates").build())
                .type("application/xml; charset=UTF-8").header(HEADER_NAME_AUTHORIZATION,
                    "Bearer " + MCRJSONWebTokenUtil.createJWTAuthorizationHeader(signedJWT))
                .build();
        }
        return response;
    }

    /**
     * serializes a map of Strings into a compact JSON structure
     * @param data a sorted Map of Strings 
     * @return a compact JSON
     */
    public static String generateMessagesFromProperties(SortedMap<String, String> data) {
        StringWriter sw = new StringWriter();
        sw.append("{");
        for (String key : data.keySet()) {
            sw.append("\"").append(key).append("\"").append(":").append("\"").append(data.get(key)).append("\"")
                .append(",");
        }
        String result = sw.toString();
        if (result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        result = result + "}";

        return result;
    }

    /**
     * verifies a set of Properties against a signature and and a public key
     * @param data - the data a sorted Map of Strings
     * @param base64Signature - the signature
     * @param jwk -the public key
     * @return true, if the properties match the signature
     */
    public static boolean verifyPropertiesWithSignature(SortedMap<String, String> data, String base64Signature,
        JWK jwk) {
        try {
            String message = generateMessagesFromProperties(data);

            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(((RSAKey) jwk).toRSAPublicKey());
            signature.update(message.getBytes(StandardCharsets.ISO_8859_1));

            return signature.verify(Base64.getDecoder().decode(base64Signature));

        } catch (Exception e) {
            LOGGER.error(e);
        }
        return false;
    }
}
