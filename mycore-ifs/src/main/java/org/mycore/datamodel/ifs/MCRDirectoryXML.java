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

package org.mycore.datamodel.ifs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;

/**
 * @author Thomas Scheffler (yagee)
 * @author Frank Lützenkirchen
 * 
 * @version $Revision$ $Date$
 */
public class MCRDirectoryXML {

    static Logger LOGGER = LogManager.getLogger(MCRDirectoryXML.class);

    private static final String dateFormat = "dd.MM.yyyy HH:mm:ss";

    private static final DateFormat dateFormatter = new SimpleDateFormat(dateFormat, Locale.ROOT);

    private static final String timeFormat = "HH:mm:ss";

    private static final DateFormat timeFormatter = new SimpleDateFormat(timeFormat, Locale.ROOT);

    protected static MCRDirectoryXML SINGLETON = new MCRDirectoryXML();

    /** 
     * If true, XML output of FileNodeServlet directory listing and 
     * IFS resolver will include additional data of extenders.
     * 
     * Set with MCR.IFS.IncludeAdditionalDataByDefault, default is false 
     */
    protected static final boolean WITH_ADDITIONAL_DATA_DEFAULT;

    static {
        WITH_ADDITIONAL_DATA_DEFAULT = MCRConfiguration.instance().getBoolean("MCR.IFS.IncludeAdditionalDataByDefault",
            false);
    }

    /**
     *  
     */
    private MCRDirectoryXML() {
        super();
    }

    public static MCRDirectoryXML getInstance() {
        return SINGLETON;
    }

    /**
     * Sends the contents of an MCRDirectory as XML data to the client
     */
    protected Document getDirectoryXML(MCRDirectory dir) {
        return getDirectoryXML(dir, WITH_ADDITIONAL_DATA_DEFAULT);
    }

    /**
     * Sends the contents of an MCRDirectory as XML data to the client
     */
    protected Document getDirectoryXML(MCRDirectory dir, boolean withAdditionalData) {
        LOGGER.info("MCRDirectoryXML: start listing of directory {}", dir.getName());

        Element root = new Element("mcr_directory");
        Document doc = new org.jdom2.Document(root);

        root.setAttribute("ID", dir.getID());

        addString(root, "path", dir.getPath());
        addString(root, "ownerID", dir.getOwnerID());
        addDate(root, "lastModified", dir.getLastModified());
        addString(root, "size", String.valueOf(dir.getSize()));

        String label = dir.getLabel();
        if (label != null) {
            addString(root, "label", label);
        }

        Element numChildren = new Element("numChildren");
        root.addContent(numChildren);

        Element ncHere = new Element("here");
        numChildren.addContent(ncHere);

        addString(ncHere, "directories",
            String.valueOf(dir.getNumChildren(MCRDirectory.DIRECTORIES, MCRDirectory.HERE)));
        addString(ncHere, "files", String.valueOf(dir.getNumChildren(MCRDirectory.FILES, MCRDirectory.HERE)));

        Element ncTotal = new Element("total");
        numChildren.addContent(ncTotal);

        addString(ncTotal, "directories",
            String.valueOf(dir.getNumChildren(MCRDirectory.DIRECTORIES, MCRDirectory.TOTAL)));
        addString(ncTotal, "files", String.valueOf(dir.getNumChildren(MCRDirectory.FILES, MCRDirectory.TOTAL)));

        Element nodes = new Element("children");
        root.addContent(nodes);

        MCRFilesystemNode[] children = dir.getChildren();

        for (MCRFilesystemNode element : children) {
            Element node = new Element("child");
            node.setAttribute("ID", element.getID());
            nodes.addContent(node);

            addString(node, "name", element.getName());

            label = element.getLabel();
            if (label != null) {
                addString(node, "label", label);
            }

            addString(node, "size", String.valueOf(element.getSize()));
            addDate(node, "lastModified", element.getLastModified());

            if (element instanceof MCRFile) {
                node.setAttribute("type", "file");

                MCRFile file = (MCRFile) element;
                addString(node, "contentType", file.getContentTypeID());
                addString(node, "md5", file.getMD5());

                if (file.hasAudioVideoExtender()) {
                    MCRAudioVideoExtender ext = file.getAudioVideoExtender();

                    Element xExtender = new Element("extender");
                    node.addContent(xExtender);
                    addExtenderData(xExtender, ext);
                }
            } else {
                node.setAttribute("type", "directory");
            }

            if (withAdditionalData) {
                try {
                    MCRContent additional = element.getAllAdditionalData();
                    if (additional != null) {
                        node.addContent(additional.asXML().detachRootElement());
                    }
                } catch (Exception ignored) {
                }
            }
        }

        LOGGER.info("MCRDirectoryXML: end listing of directory {}", dir.getName());

        return doc;

    }

    private void addDate(Element parent, String type, GregorianCalendar date) {
        Element xDate = new Element("date");
        parent.addContent(xDate);

        xDate.setAttribute("type", type);

        String time = dateFormatter.format(date.getTime());

        xDate.setAttribute("format", dateFormat);
        xDate.addContent(time);
    }

    private void addString(Element parent, String itemName, String content) {
        if (content == null || content.trim().length() == 0) {
            return;
        }

        parent.addContent(new Element(itemName).addContent(content.trim()));
    }

    private void addExtenderData(Element parent, MCRAudioVideoExtender ext) {
        parent.setAttribute("type", ext.hasVideoStream() ? "video" : "audio");

        int hh = ext.getDurationHours();
        int mm = ext.getDurationMinutes();
        int ss = ext.getDurationSeconds();
        addTime(parent, "duration", hh, mm, ss);

        addString(parent, "bitRate", String.valueOf(ext.getBitRate()));

        if (ext.hasVideoStream()) {
            addString(parent, "frameRate", String.valueOf(ext.getFrameRate()));
        }

        addString(parent, "playerURL", ext.getPlayerDownloadURL());
    }

    private void addTime(Element parent, String type, int hh, int mm, int ss) {
        Element xTime = new Element(type);
        parent.addContent(xTime);

        GregorianCalendar date = new GregorianCalendar(TimeZone.getDefault(), Locale.ROOT);
        date.set(2002, Calendar.FEBRUARY, 1, hh, mm, ss);
        String time = timeFormatter.format(date.getTime());

        xTime.setAttribute("format", timeFormat);
        xTime.addContent(time);
    }

    /**
     * Handles the HTTP request
     */
    public Document getDirectory(String requestPath) {
        return getDirectory(requestPath, WITH_ADDITIONAL_DATA_DEFAULT);
    }

    /**
     * Handles the HTTP request
     */
    public Document getDirectory(String requestPath, boolean withAdditionalData) {
        LOGGER.info("MCRDirectoryResolver: request path = {}", requestPath);

        if (requestPath == null) {
            String msg = "Error: request path is null";
            LOGGER.error(msg);

            return getErrorDocument(msg);
        }

        StringTokenizer st = new StringTokenizer(requestPath, "/");

        if (!st.hasMoreTokens()) {
            String msg = "Error: request path is empty";
            LOGGER.error(msg);

            return getErrorDocument(msg);
        }

        String ownerID = st.nextToken();

        // local node to be retrieved
        MCRFilesystemNode root;

        try {
            root = MCRFilesystemNode.getRootNode(ownerID);
        } catch (org.mycore.common.MCRPersistenceException e) {
            // Could not get value from JDBC result set
            LOGGER.error("MCRFileNodeServlet: Error while getting root node!", e);
            root = null;
        }

        if (root == null) {
            String msg = "Error: No root node found for owner ID " + ownerID;
            LOGGER.error(msg);

            return getErrorDocument(msg);
        }

        if (root instanceof MCRFile) {
            String msg = "Error: root node is a file: " + root.getName();
            LOGGER.error(msg);

            return getErrorDocument(msg);
        }

        // root node is a directory
        int pos = ownerID.length() + 1;
        String path = requestPath.substring(pos);

        if (path.length() == 0) {
            // only owner ID submitted
            return getDirectoryXML((MCRDirectory) root, withAdditionalData);
        }

        MCRDirectory dir = (MCRDirectory) root;
        MCRFilesystemNode node = dir.getChildByPath(path);

        if (node == null) {
            String msg = "Error: No such file or directory " + path;
            LOGGER.error(msg);

            return getErrorDocument(msg);
        } else if (node instanceof MCRFile) {
            String msg = "Error: node is a file: " + root.getName();
            LOGGER.error(msg);

            return getErrorDocument(msg);
        } else {
            return getDirectoryXML((MCRDirectory) node, withAdditionalData);
        }
    }

    /**
     * returns a error document to display error messages
     * TODO:should be extended to provide stacktraces etc.
     * @return JDOM Document with root element "mcr_error"
     */
    private Document getErrorDocument(String msg) {
        return new Document(new Element("mcr_error").setText(msg));
    }

}
