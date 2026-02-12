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

package org.mycore.services.zipper;

import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRXlink;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectStructure;
import org.mycore.datamodel.metadata.MCRXMLConstants;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This servlet delivers the contents of MCROjects to the client in
 * container files (see classes extending this servlet). Read permission is required.
 * There are three modes
 * <ol>
 *  <li>if id=mycoreobjectID (delivers the metadata, including all derivates)</li>
 *  <li>if id=derivateID (delivers all files of the derivate)</li>
 *  <li>if id=derivateID/directoryPath (delivers all files in the given directory of the derivate)</li>
 * </ol>
 * 
 * "id" maybe specified as {@link HttpServletRequest#getPathInfo()} or as
 * {@link MCRServlet#getProperty(HttpServletRequest, String)}.
 * @author Thomas Scheffler (yagee)
 *
 */
public abstract class MCRCompressServlet<T extends AutoCloseable> extends MCRServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String KEY_OBJECT_ID = MCRCompressServlet.class.getCanonicalName() + ".object";

    private static final String KEY_PATH = MCRCompressServlet.class.getCanonicalName() + ".path";

    private static final Pattern PATH_INFO_PATTERN = Pattern.compile("\\A([\\w]+)/([\\w/]+)\\z");

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void think(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        //id parameter for backward compatibility
        String paramid = getProperty(req, "id");
        if (paramid == null) {
            String pathInfo = req.getPathInfo();
            if (pathInfo != null) {
                paramid = pathInfo.substring(1);
            }
        }
        if (paramid == null) {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "What should I do?");
            return;
        }
        Matcher ma = PATH_INFO_PATTERN.matcher(paramid);
        //path & directory
        MCRObjectID id;
        String path;
        try {
            if (ma.find()) {
                id = MCRObjectID.getInstance(ma.group(1));
                path = ma.group(2);
            } else {
                id = MCRObjectID.getInstance(paramid);
                path = null;
            }
        } catch (MCRException e) {
            String objId = ma.find() ? ma.group(1) : paramid;
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "ID is not valid: " + objId);
            return;
        }
        boolean readPermission = id.getTypeId().equals(MCRDerivate.OBJECT_TYPE) ? MCRAccessManager
            .checkDerivateContentPermission(id, PERMISSION_READ)
            : MCRAccessManager.checkPermission(id, PERMISSION_READ);
        if (!readPermission) {
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, "You may not read " + id);
            return;
        }
        req.setAttribute(KEY_OBJECT_ID, id);
        req.setAttribute(KEY_PATH, path);
    }

    @Override
    protected void render(MCRServletJob job, Exception ex) throws Exception {
        if (ex != null) {
            //we cannot handle it ourself
            throw ex;
        }
        if (job.getResponse().isCommitted()) {
            return;
        }
        MCRObjectID id = (MCRObjectID) job.getRequest().getAttribute(KEY_OBJECT_ID);
        String path = (String) job.getRequest().getAttribute(KEY_PATH);
        try (ServletOutputStream sout = job.getResponse().getOutputStream()) {
            StringBuffer requestURL = job.getRequest().getRequestURL();
            if (job.getRequest().getQueryString() != null) {
                requestURL.append('?').append(job.getRequest().getQueryString());
            }
            MCRISO8601Date mcriso8601Date = new MCRISO8601Date();
            mcriso8601Date.setDate(new Date());
            String comment = "Created by " + requestURL + " at " + mcriso8601Date.getISOString();
            try (T container = createContainer(sout, comment)) {
                job.getResponse().setContentType(getMimeType());
                String filename = getFileName(id, path);
                job.getResponse().addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                if (id.getTypeId().equals(MCRDerivate.OBJECT_TYPE)) {
                    sendDerivate(id, path, container);
                } else {
                    sendObject(id, job, container);
                }
                disposeContainer(container);
            }
        }
    }

    private void sendObject(MCRObjectID id, MCRServletJob job, T container) throws Exception {
        MCRContent content = MCRXMLMetadataManager.getInstance().retrieveContent(id);
        if (content == null) {
            throw new FileNotFoundException("Could not find object: " + id);
        }
        long lastModified = MCRXMLMetadataManager.getInstance().getLastModified(id);
        HttpServletRequest req = job.getRequest();
        byte[] metaDataContent = getMetaDataContent(content, req);
        sendMetadataCompressed("metadata.xml", metaDataContent, lastModified, container);

        // zip all derivates
        List<Element> li = content.asXML().getRootElement().getChild(MCRObjectStructure.XML_NAME)
            .getChild(MCRObjectStructure.ELEMENT_DERIVATE_OBJECTS)
            .getChildren("derobject");

        XPathFactory xpathFactory = XPathFactory.instance();
        XPathExpression<Attribute> derivateTypeXpath = xpathFactory
            .compile("classification[@classid='derivate_types']/@categid",
                Filters.attribute(), null, MCRConstants.XML_NAMESPACE);
        XPathExpression<Boolean> exportedXpath = xpathFactory
            .compile(".//category/label[lang('x-export')]/@text='false'",
                Filters.fboolean(), null, MCRConstants.XML_NAMESPACE);
        
        for (Element el : li) {
            if (el.getAttributeValue(MCRXMLConstants.INHERITED).equals("0")) {
                String ownerID = el.getAttributeValue(MCRXlink.HREF, XLINK_NAMESPACE);
                MCRObjectID derId = MCRObjectID.getInstance(ownerID);
                // here the access check is tested only against the derivate
                if (MCRAccessManager.checkDerivateContentPermission(derId, PERMISSION_READ)) {

                    // export derivate if no derivate type has an 'x-export' property of 'false'
                    boolean exported = derivateTypeXpath.evaluate(el).stream().noneMatch(derivateTypeAttribute -> {
                        String derivateType = derivateTypeAttribute.getValue();
                        Element derivateTypeElement = MCRURIResolver.obtainInstance()
                            .resolve("classification:metadata:0:children:derivate_types:" + derivateType);
                        return exportedXpath.evaluate(derivateTypeElement).get(0);
                    });

                    if (exported) {
                        sendDerivate(derId, null, container);
                    }
                }
            }
        }
    }

    private byte[] getMetaDataContent(MCRContent content, HttpServletRequest req) throws Exception {
        // zip the object's Metadata
        MCRParameterCollector parameters = new MCRParameterCollector(req);
        if (parameters.getParameter("Style", null) == null) {
            parameters.setParameter("Style", "compress");
        }
        MCRContentTransformer contentTransformer = MCRLayoutService.getContentTransformer(content.getDocType(),
            parameters);
        ByteArrayOutputStream out = new ByteArrayOutputStream(32 * 1024);
        if (contentTransformer instanceof MCRParameterizedTransformer parameterizedTransformer) {
            parameterizedTransformer.transform(content, out, parameters);
        } else {
            contentTransformer.transform(content, out);
        }
        return out.toByteArray();
    }

    private void sendDerivate(MCRObjectID id, String path, T container) throws IOException {

        MCRPath resolvedPath = MCRPath.getPath(id.toString(), path == null ? "/" : path);

        if (!Files.exists(resolvedPath)) {
            throw new NoSuchFileException(id.toString(), path, "Could not find path " + resolvedPath);
        }

        if (Files.isRegularFile(resolvedPath)) {
            BasicFileAttributes attrs = Files.readAttributes(resolvedPath, BasicFileAttributes.class);
            sendCompressedFile(resolvedPath, attrs, container);
            LOGGER.debug("file {} zipped", resolvedPath);
            return;
        }
        // root is a directory
        Files.walkFileTree(resolvedPath, new CompressVisitor<>(this, container));
    }

    private String getFileName(MCRObjectID id, String path) {
        if (path == null || path.equals("")) {
            return new MessageFormat("{0}.{1}", Locale.ROOT).format(new Object[] { id, getFileExtension() });
        } else {
            return new MessageFormat("{0}-{1}.{2}", Locale.ROOT).format(
                new Object[] { id, path.replaceAll("/", "-"), getFileExtension() });
        }
    }

    /**
     * Constructs a path name in form of {ownerID}+'/'+{path} or {ownerID} if path is root component.
     * @param path absolute path
     */
    protected String getFilename(MCRPath path) {
        return path.getNameCount() == 0 ? path.getOwner()
            : path.getOwner() + '/'
                + path.getRoot().relativize(path);
    }

    protected abstract void sendCompressedDirectory(MCRPath file, BasicFileAttributes attrs, T container)
        throws IOException;

    protected abstract void sendCompressedFile(MCRPath file, BasicFileAttributes attrs, T container) throws IOException;

    protected abstract void sendMetadataCompressed(String fileName, byte[] content, long modified, T container)
        throws IOException;

    protected abstract String getMimeType();

    protected abstract String getFileExtension();

    protected abstract T createContainer(ServletOutputStream sout, String comment);

    protected abstract void disposeContainer(T container) throws IOException;

    private static class CompressVisitor<T extends AutoCloseable> extends SimpleFileVisitor<Path> {

        private MCRCompressServlet<T> impl;

        private T container;

        CompressVisitor(MCRCompressServlet<T> impl, T container) {
            this.impl = impl;
            this.container = container;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            impl.sendCompressedDirectory(MCRPath.ofPath(dir), attrs, container);
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            impl.sendCompressedFile(MCRPath.ofPath(file), attrs, container);
            LOGGER.debug("file {} zipped", file);
            return super.visitFile(file, attrs);
        }

    }

}
