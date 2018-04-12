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

package org.mycore.wcms2.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.mycore.frontend.MCRLayoutUtilities;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.wcms2.MCRWebPagesSynchronizer;
import org.mycore.wcms2.access.MCRWCMSPermission;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Path("wcms2/filebrowser")
@MCRRestrictedAccess(MCRWCMSPermission.class)
public class MCRWCMSFileBrowserResource {

    private ArrayList<String> folderList = new ArrayList<>();

    private String wcmsDataPath = MCRWebPagesSynchronizer.getWCMSDataDir().getPath();

    private static final Logger LOGGER = LogManager.getLogger(MCRWCMSFileBrowserResource.class);

    @Context
    HttpServletRequest request;

    @Context
    HttpServletResponse response;

    @Context
    ServletContext context;

    @GET
    public InputStream getFileBrowser() throws Exception {
        return getClass().getResourceAsStream("/META-INF/resources/modules/wcms2/filebrowser.html");
    }

    @GET
    @Path("gui/{filename:.*}")
    public Response getResources(@PathParam("filename") String filename) {
        if (filename.endsWith(".js")) {
            return Response.ok(getClass()
                .getResourceAsStream("/META-INF/resources/modules/wcms2/" + filename))
                .header("Content-Type", "application/javascript")
                .build();
        }

        if (filename.endsWith(".css")) {
            return Response.ok(getClass()
                .getResourceAsStream("/META-INF/resources/modules/wcms2/" + filename))
                .header("Content-Type", "text/css")
                .build();
        }
        return Response.ok(getClass()
            .getResourceAsStream("/META-INF/resources/modules/wcms2/" + filename))
            .build();
    }

    @GET
    @Path("/folder")
    public String getFolders() throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        File file = new File(MCRLayoutUtilities.getNavigationURL().getPath());
        Document doc = docBuilder.parse(file);
        getallowedPaths(doc.getDocumentElement());

        File dir = MCRWebPagesSynchronizer.getWCMSDataDir();
        JsonObject jsonObj = new JsonObject();
        jsonObj.add("folders", getFolder(dir, false));
        return jsonObj.toString();
    }

    @POST
    @Path("/folder")
    public Response addFolder(@QueryParam("path") String path) throws IOException {
        File wcmsDir = new File(MCRWebPagesSynchronizer.getWCMSDataDir().getPath() + path);
        File wepAppdir = new File(MCRWebPagesSynchronizer.getWebAppBaseDir().getPath() + path);
        if (wcmsDir.mkdir() && wepAppdir.mkdir()) {
            return Response.ok().build();
        }
        return Response.status(Status.CONFLICT).build();
    }

    @DELETE
    @Path("/folder")
    public Response deleteFolder(@QueryParam("path") String path) throws IOException {
        File wcmsDir = new File(MCRWebPagesSynchronizer.getWCMSDataDir().getPath() + path);
        File wepAppdir = new File(MCRWebPagesSynchronizer.getWebAppBaseDir().getPath() + path);

        if (wepAppdir.isDirectory()) {
            if (wepAppdir.list().length < 1) {
                if (FileUtils.deleteQuietly(wepAppdir) && FileUtils.deleteQuietly(wcmsDir)) {
                    return Response.ok().build();
                }
            } else {
                return Response.status(Status.FORBIDDEN).build();
            }
        }
        return Response.status(Status.CONFLICT).build();
    }

    @GET
    @Path("/files")
    public String getFiles(@QueryParam("path") String path, @QueryParam("type") String type) throws IOException {
        File dir = new File(MCRWebPagesSynchronizer.getWCMSDataDir().getPath() + path);
        JsonObject jsonObj = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        File[] fileArray = dir.listFiles();
        if (fileArray != null) {
            for (File file : fileArray) {
                String mimetype = Files.probeContentType(file.toPath());
                if (mimetype != null && (type.equals("images") ? mimetype.split("/")[0].equals("image")
                    : !mimetype.split("/")[1].contains("xml"))) {
                    JsonObject fileJsonObj = new JsonObject();
                    fileJsonObj.addProperty("name", file.getName());
                    fileJsonObj.addProperty("path", context.getContextPath() + path + "/" + file.getName());
                    if (file.isDirectory()) {
                        fileJsonObj.addProperty("type", "folder");
                    } else {
                        fileJsonObj.addProperty("type", mimetype.split("/")[0]);
                    }
                    jsonArray.add(fileJsonObj);
                }
            }
            jsonObj.add("files", jsonArray);
            return jsonObj.toString();
        }
        return "";
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
    public Response deleteFile(@QueryParam("path") String path) throws IOException {
        File wcmsDir = new File(MCRWebPagesSynchronizer.getWCMSDataDir().getPath() + path);
        File wepAppdir = new File(MCRWebPagesSynchronizer.getWebAppBaseDir().getPath() + path);
        if (delete(wcmsDir) && delete(wepAppdir)) {
            return Response.ok().build();
        }
        return Response.status(Status.CONFLICT).build();
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String getUpload(@FormDataParam("upload") InputStream inputStream,
        @FormDataParam("upload") FormDataContentDisposition header, @QueryParam("CKEditorFuncNum") int funcNum,
        @QueryParam("href") String href, @QueryParam("type") String type, @QueryParam("basehref") String basehref) {
        String path = "";
        try {
            path = saveFile(inputStream, href + "/" + header.getFileName());
        } catch (IOException e) {
            LOGGER.error("Error while saving {}", href + "/" + header.getFileName(), e);
            return "";
        }
        if ("images".equals(type)) {
            return "<script type='text/javascript'>window.parent.CKEDITOR.tools.callFunction(" + funcNum + ",'"
                + path.substring(path.lastIndexOf("/") + 1, path.length()) + "', '');</script>";
        }
        return "<script type='text/javascript'>window.parent.CKEDITOR.tools.callFunction(" + funcNum + ",'" + basehref
            + path.substring(path.lastIndexOf("/") + 1, path.length()) + "', '');</script>";
    }

    protected String saveFile(InputStream inputStream, String path) throws IOException {
        String newPath = testIfFileExists(path);
        OutputStream outputStream = MCRWebPagesSynchronizer.getOutputStream(newPath);
        int read = 0;
        byte[] bytes = new byte[1024];

        while ((read = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read);
        }
        outputStream.flush();
        outputStream.close();
        return newPath;
    }

    protected String testIfFileExists(String path) {
        String newPath = path;
        File file = new File(MCRWebPagesSynchronizer.getWCMSDataDir().getPath() + newPath);
        int i = 1;
        while (file.exists()) {
            String type = newPath.substring(newPath.lastIndexOf("."));
            String name = newPath.substring(0, newPath.lastIndexOf("."));
            if (i > 1) {
                name = name.substring(0, name.lastIndexOf("("));
            }
            newPath = name + "(" + i++ + ")" + type;
            file = new File(MCRWebPagesSynchronizer.getWCMSDataDir().getPath() + newPath);
        }
        return newPath;
    }

    protected void getallowedPaths(Element element) {
        String pathString = element.getAttribute("dir");
        if (!"".equals(pathString)) {
            folderList.add(wcmsDataPath + pathString);
        }
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                getallowedPaths((Element) node);
            }
        }
    }

    protected JsonObject getFolder(File node, boolean folderallowed) {
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("name", node.getName());
        jsonObj.addProperty("path", node.getAbsolutePath());
        jsonObj.addProperty("allowed", folderallowed);
        if (node.isDirectory()) {
            jsonObj.addProperty("type", "folder");
            JsonArray jsonArray = new JsonArray();
            File[] childNodes = node.listFiles();
            for (File child : childNodes) {
                if (child.isDirectory()) {
                    if (folderallowed || folderList.contains(child.getPath())) {
                        jsonArray.add(getFolder(child, true));
                    } else {
                        jsonArray.add(getFolder(child, false));
                    }
                }
            }
            if (jsonArray.size() > 0) {
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
