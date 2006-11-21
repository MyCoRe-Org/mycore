/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.datamodel.ifs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;

/**
 * @author Thomas Scheffler (yagee)
 * @author Frank Lützenkirchen
 * 
 * @version $Revision$ $Date$
 */
public class MCRDirectoryXML {

    static Logger LOGGER = Logger.getLogger(MCRDirectoryXML.class);

    private static final String dateFormat = "dd.MM.yyyy HH:mm:ss";

    private static final DateFormat dateFormatter = new SimpleDateFormat(dateFormat);

    private static final String timeFormat = "HH:mm:ss";

    private static final DateFormat timeFormatter = new SimpleDateFormat(timeFormat);

    private static MCRConfiguration CONFIG = MCRConfiguration.instance();

    private static ArrayList remoteAliasList;

    protected static MCRDirectoryXML SINGLETON;

    /**
     *  
     */
    private MCRDirectoryXML() {
        super();
        init();
    }

    public static MCRDirectoryXML getInstance() {
        if (SINGLETON == null) {
            SINGLETON = new MCRDirectoryXML();
        }
        return SINGLETON;
    }

    /**
     * Sends the contents of an MCRDirectory as XML data to the client
     */
    protected Document getDirectoryXML(MCRDirectory dir){
        LOGGER.info("MCRDirectoryXML: listing of directory " + dir.getName());

        Element root = new Element("mcr_directory");
        Document doc = new org.jdom.Document(root);

        root.setAttribute("ID", dir.getID());

        addString(root, "path", dir.getPath());
        addString(root, "ownerID", dir.getOwnerID());
        addDate(root, "lastModified", dir.getLastModified());
        addString(root, "size", String.valueOf(dir.getSize()));

        Element numChildren = new Element("numChildren");
        root.addContent(numChildren);

        Element ncHere = new Element("here");
        numChildren.addContent(ncHere);

        addString(ncHere, "directories", String.valueOf(dir.getNumChildren(MCRDirectory.DIRECTORIES, MCRDirectory.HERE)));
        addString(ncHere, "files", String.valueOf(dir.getNumChildren(MCRDirectory.FILES, MCRDirectory.HERE)));

        Element ncTotal = new Element("total");
        numChildren.addContent(ncTotal);

        addString(ncTotal, "directories", String.valueOf(dir.getNumChildren(MCRDirectory.DIRECTORIES, MCRDirectory.TOTAL)));
        addString(ncTotal, "files", String.valueOf(dir.getNumChildren(MCRDirectory.FILES, MCRDirectory.TOTAL)));

        Element nodes = new Element("children");
        root.addContent(nodes);

        MCRFilesystemNode[] children = dir.getChildren();

        for (int i = 0; i < children.length; i++) {
            Element node = new Element("child");
            node.setAttribute("ID", children[i].getID());
            nodes.addContent(node);

            addString(node, "name", children[i].getName());
            addString(node, "size", String.valueOf(children[i].getSize()));
            addDate(node, "lastModified", children[i].getLastModified());

            if (children[i] instanceof MCRFile) {
                node.setAttribute("type", "file");

                MCRFile file = (MCRFile) (children[i]);
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
        }

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
        if ((content == null) || (content.trim().length() == 0)) {
            return;
        }

        parent.addContent(new Element(itemName).addContent(content.trim()));
    }

    private void addExtenderData(Element parent, MCRAudioVideoExtender ext) {
        parent.setAttribute("type", ext.isVideo() ? "video" : "audio");

        int hh = ext.getDurationHours();
        int mm = ext.getDurationMinutes();
        int ss = ext.getDurationSeconds();
        addTime(parent, "duration", hh, mm, ss);

        addString(parent, "bitRate", String.valueOf(ext.getBitRate()));

        if (ext.isVideo()) {
            addString(parent, "frameRate", String.valueOf(ext.getFrameRate()));
        }

        addString(parent, "playerURL", ext.getPlayerDownloadURL());
    }

    private void addTime(Element parent, String type, int hh, int mm, int ss) {
        Element xTime = new Element(type);
        parent.addContent(xTime);

        GregorianCalendar date = new GregorianCalendar(2002, 01, 01, hh, mm, ss);
        String time = timeFormatter.format(date.getTime());

        xTime.setAttribute("format", timeFormat);
        xTime.addContent(time);
    }

    /**
     * Handles the HTTP request
     */
    public Document getDirectory(String requestPath, String hostAlias) {

        hostAlias = "local"; //TODO remove hostAlias

        LOGGER.debug("MCRFileNodeServlet : host = " + hostAlias);
        LOGGER.info("MCRDirectoryResolver: request path = " + requestPath);

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
            return getDirectoryXML((MCRDirectory) root);
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
            return getDirectoryXML((MCRDirectory) node);
        }
    }

    /**
     * Initializes the servlet and reads the default language and the remote
     * host list from the configuration.
     */
    public void init() {

        // read host list from configuration
        String hostconf = CONFIG.getString("MCR.remoteaccess_hostaliases", "local");
        remoteAliasList = new ArrayList();

        if (hostconf.indexOf("local") < 0) {
            remoteAliasList.add("local");
        }

        StringTokenizer st = new StringTokenizer(hostconf, ", ");

        while (st.hasMoreTokens())
            remoteAliasList.add(st.nextToken());
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
