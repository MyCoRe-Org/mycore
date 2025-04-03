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

package org.mycore.wcms2.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.mycore.common.MCRUtils;
import org.mycore.frontend.MCRLayoutUtilities;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.resource.MCRResourceHelper;
import org.mycore.wcms2.MCRWCMSUtil;
import org.mycore.wcms2.access.MCRWCMSPermission;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("wcms2/filebrowser")
@MCRRestrictedAccess(MCRWCMSPermission.class)
public class MCRWCMSFileBrowserResource {

    private final List<String> folderList = new ArrayList<>();

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String QUERY_PARAM_PATH = "path";

    private static final String QUERY_PARAM_TYPE = "type";

    public static final String JSON_PROPERTY_PATH = "path";

    public static final String JSON_PROPERTY_NAME = "name";

    public static final String JSON_PROPERTY_TYPE = "type";

    @Context
    ServletContext context;

    @GET
    public InputStream getFileBrowser() {
        return getWCMSResource("filebrowser.html");
    }

    @GET
    @Path("gui/{filename:.*}")
    public Response getResources(@PathParam("filename") String filename) {
        if (filename.startsWith("/") || filename.contains("../")) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        if (filename.endsWith(".js")) {
            return Response.ok(getWCMSResource(filename))
                .header("Content-Type", "application/javascript")
                .build();
        }

        if (filename.endsWith(".css")) {
            return Response.ok(getWCMSResource(filename))
                .header("Content-Type", "text/css")
                .build();
        }

        return Response.ok(getWCMSResource(filename))
            .build();
    }

    private InputStream getWCMSResource(String filename) {
        return MCRResourceHelper.getWebResourceAsStream("/modules/wcms2/" + filename);
    }

    @GET
    @Path("/folder")
    public String getFolders() throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        File file = new File(MCRLayoutUtilities.getNavigationURL().getPath());
        Document doc = docBuilder.parse(file);
        getAllowedPaths(doc.getDocumentElement());

        File dir = MCRWCMSUtil.getWCMSDataDir();
        JsonObject jsonObj = new JsonObject();
        jsonObj.add("folders", getFolder(dir, false));
        return jsonObj.toString();
    }

    @POST
    @Path("/folder")
    public Response addFolder(@QueryParam(QUERY_PARAM_PATH) String path) throws IOException {
        File wcmsDir = resolveDirWCMS(path);
        if (wcmsDir.mkdir()) {
            return Response.ok().build();
        }
        return Response.status(Status.CONFLICT).build();
    }

    @DELETE
    @Path("/folder")
    public Response deleteFolder(@QueryParam(QUERY_PARAM_PATH) String path) {
        File wcmsDir = resolveDirWCMS(path);
        if (FileUtils.deleteQuietly(wcmsDir)) {
            return Response.ok().build();
        }
        return Response.status(Status.CONFLICT).build();
    }

    private File resolveDirWCMS(String path) {
        return MCRUtils.safeResolve(MCRWCMSUtil.getWCMSDataDirPath(), removeLeadingSlash(path)).toFile();
    }

    @GET
    @Path("/files")
    public String getFiles(@QueryParam(QUERY_PARAM_PATH) String path, @QueryParam(QUERY_PARAM_TYPE) String type)
        throws IOException {
        File dir = MCRUtils.safeResolve(MCRWCMSUtil.getWCMSDataDirPath(), removeLeadingSlash(path)).toFile();
        JsonObject jsonObj = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        File[] fileArray = dir.listFiles();
        if (fileArray != null) {
            for (File file : fileArray) {
                String mimetype = Files.probeContentType(file.toPath());
                if (mimetype != null && (type.equals("images") ? mimetype.split("/")[0].equals("image")
                    : !mimetype.split("/")[1].contains("xml"))) {
                    JsonObject fileJsonObj = getJsonFileObject(path, file, mimetype);
                    jsonArray.add(fileJsonObj);
                }
            }
            jsonObj.add("files", jsonArray);
            return jsonObj.toString();
        }
        return "";
    }

    private JsonObject getJsonFileObject(String path, File file, String mimetype) {
        JsonObject fileJsonObj = new JsonObject();
        fileJsonObj.addProperty(JSON_PROPERTY_NAME, file.getName());
        fileJsonObj.addProperty(JSON_PROPERTY_PATH, context.getContextPath() + path + "/" + file.getName());
        if (file.isDirectory()) {
            fileJsonObj.addProperty(JSON_PROPERTY_TYPE, "folder");
        } else {
            fileJsonObj.addProperty(JSON_PROPERTY_TYPE, mimetype.split("/")[0]);
        }
        return fileJsonObj;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response getUpload(@FormDataParam("file") InputStream inputStream,
        @FormDataParam("file") FormDataContentDisposition header, @FormDataParam("path") String path) {
        try {
            saveFile(inputStream, path + "/" + header.getFileName());
        } catch (IOException e) {
            LOGGER.error("Error while saving {}", path, e);
            return Response.status(Status.CONFLICT).build();
        }
        return Response.ok().build();
    }

    @DELETE
    public Response deleteFile(@QueryParam(QUERY_PARAM_PATH) String path) {
        File wcmsDir = MCRUtils.safeResolve(MCRWCMSUtil.getWCMSDataDirPath(), removeLeadingSlash(path)).toFile();
        if (delete(wcmsDir)) {
            return Response.ok().build();
        }
        return Response.status(Status.CONFLICT).build();
    }

    protected void saveFile(InputStream inputStream, String path) throws IOException {
        String newPath = testIfFileExists(path);
        OutputStream outputStream = MCRWCMSUtil.getOutputStream(newPath);
        byte[] bytes = new byte[1024];
        int read = inputStream.read(bytes);
        while (read != -1) {
            outputStream.write(bytes, 0, read);
            read = inputStream.read(bytes);
        }
        outputStream.flush();
        outputStream.close();
    }

    protected String testIfFileExists(String path) {
        String newPath = removeLeadingSlash(path);
        java.nio.file.Path basePath = MCRWCMSUtil.getWCMSDataDirPath();
        File file = MCRUtils.safeResolve(basePath, newPath)
            .toFile();
        int i = 1;
        while (file.exists()) {
            String type = newPath.substring(newPath.lastIndexOf('.'));
            String name = newPath.substring(0, newPath.lastIndexOf('.'));
            if (i > 1) {
                name = name.substring(0, name.lastIndexOf('('));
            }
            newPath = name + "(" + i++ + ")" + type;
            file = MCRUtils.safeResolve(basePath, newPath).toFile();
        }
        return newPath;
    }

    private String removeLeadingSlash(String newPath) {
        return newPath.startsWith("/") ? newPath.substring(1) : newPath;
    }

    protected void getAllowedPaths(Element element) {
        String pathString = element.getAttribute("dir");
        if (!Objects.equals(pathString, "")) {
            folderList.add(MCRWCMSUtil.getWCMSDataDir().getPath() + pathString);
        }
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                getAllowedPaths((Element) node);
            }
        }
    }

    protected JsonObject getFolder(File node, boolean folderAllowed) {
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty(JSON_PROPERTY_NAME, node.getName());
        jsonObj.addProperty(JSON_PROPERTY_PATH, node.getAbsolutePath());
        jsonObj.addProperty("allowed", folderAllowed);
        if (node.isDirectory()) {
            jsonObj.addProperty(JSON_PROPERTY_TYPE, "folder");
            JsonArray jsonArray = new JsonArray();
            File[] childNodes = node.listFiles();
            for (File child : childNodes) {
                if (child.isDirectory()) {
                    if (folderAllowed || folderList.contains(child.getPath())) {
                        jsonArray.add(getFolder(child, true));
                    } else {
                        jsonArray.add(getFolder(child, false));
                    }
                }
            }
            if (!jsonArray.isEmpty()) {
                jsonObj.add("children", jsonArray);
            }
            return jsonObj;
        }
        return jsonObj;
    }

    protected static boolean delete(File file) {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                delete(subFile);
            }
        }
        return !file.exists() || file.delete();
    }

}
