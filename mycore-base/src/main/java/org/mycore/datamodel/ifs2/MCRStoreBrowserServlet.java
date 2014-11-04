/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.ifs2;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * Browses the hierarchical structure of any MCRMetadataStore to allow
 * robots access to all metadata. The XML output is rendered to HTML
 * using storeBrowser.xsl. 
 * 
 * Some usage examples:
 * http://localhost:8291/storeBrowser/DocPortal_author/index.html
 * http://localhost:8291/storeBrowser/DocPortal_author/0041/09/index.html
 * 
 * The first path fragment following the servlet mapping storeBrowser/ 
 * must be the ID of the IFS2 metadata store, which is same as the
 * MCRObjectID base.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRStoreBrowserServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        String pathInfo = job.getRequest().getPathInfo();

        MCRStoreBrowserRequest sbr = new MCRStoreBrowserRequest(pathInfo);
        Document xml = sbr.buildResponseXML();

        getLayoutService().doLayout(job.getRequest(), job.getResponse(), new MCRJDOMContent(xml));
    }
}

class MCRStoreBrowserRequest {

    /** The slot path elements given from request, for example /0041/09/ */
    private List<String> pathElements = new ArrayList<String>();

    /** The store thats contents should be browsed */
    private MCRMetadataStore store;

    MCRStoreBrowserRequest(String pathInfo) throws Exception {
        StringTokenizer tokenizer = new StringTokenizer(pathInfo, "/");

        String storeID = tokenizer.nextToken();
        getStore(storeID);

        while (tokenizer.countTokens() > 1)
            pathElements.add(tokenizer.nextToken());
    }

    /** Gets the MCRMetadataStore for the given storeID */
    private void getStore(String storeID) throws Exception {
        if (storeID.contains("_"))
            store = MCRXMLMetadataManager.instance().getStore(storeID);
        if (store == null) {
            store = MCRStoreManager.getStore(storeID, MCRMetadataStore.class);
            if (store == null)
                store = MCRStoreManager.createStore(storeID, MCRMetadataStore.class);
        }
    }

    /** Builds the xml output to be rendered as response */
    Document buildResponseXML() throws Exception {
        File dir = getRequestedDirectory();

        String[] children = dir.list();
        Element xml = new Element("storeBrowser");
        if (children != null)
            for (String child : children) {
                xml.addContent(buildXML(child));
            }

        return new Document(xml);
    }

    /** 
     * Builds the xml output for a single child slot or object below the current browse level
     * 
     * @param child the file name of the slot directory or object metadata file in store
     */
    private Element buildXML(String child) throws Exception {
        return childIsSlot(child) ? buildSlotXML(child) : buildObjectXML(child);
    }

    /** 
     * Builds the xml output of a single object below the current slot directory to browse
     * 
     * @param child the file name of the object metadata file in store
     */
    private Element buildObjectXML(String child) throws IOException {
        String objectID = child.replace(".xml", "");
        int id = store.slot2id(child);
        String lastModified = getLastModifiedDate(id);

        Element xml = new Element("object");
        xml.setAttribute("id", objectID);
        xml.setAttribute("lastModified", lastModified);
        return xml;
    }

    /** 
     * Builds the xml output of a single child slot directory below the current slot to browse
     * 
     * @param child the file name of the slot directory in store
     */
    private Element buildSlotXML(String slot) {
        Element xml = new Element("slot");
        xml.setAttribute("from", buildMinimumIDContained(slot));
        xml.setAttribute("path", slot);
        return xml;
    }

    /**
     * Returns the date of last modification for the given object id in store. 
     */
    private String getLastModifiedDate(int id) throws IOException {
        Date lastModified = store.retrieve(id).getLastModified();
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(lastModified);
    }

    /**
     * Returns the minimum ID of any object that can be stored below
     * the given slot directory.
     */
    private String buildMinimumIDContained(String slot) {
        StringBuilder sb = new StringBuilder();

        for (String token : pathElements)
            sb.append(token);
        sb.append(slot);

        while (sb.length() < store.getIDLength())
            sb.append("0");

        int id = Integer.parseInt(sb.toString());
        id = Math.max(id, 1); // Minimum possible ID is 1, not 0
        return String.valueOf(id);
    }

    /**
     * Checks if the given file name represents a slot directory
     * 
     * @return true, if this is a slot directory, false if it is a object metadata file
     */
    private boolean childIsSlot(String child) {
        return child.length() < store.getIDLength();
    }

    /** Returns the directory in filesystem the requested slot path is mapped to in the store */
    private File getRequestedDirectory() {
        File dir = getBaseDir();
        for (String token : pathElements)
            dir = new File(dir, token);
        return dir;
    }

    /** Returns the base directory in the local filesystem the store to be browsed uses */
    private File getBaseDir() {
        return new File(store.getStoreConfig().getBaseDir());
    }
}
