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
import java.util.StringTokenizer;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * Browses the hierarchical structure of any MCRMetadataStore to allow
 * robots access to all metadata. The XML output is rendered to HTML
 * using storeBrowser.xsl.  
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRStoreBrowserServlet extends MCRServlet {

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        String pathInfo = job.getRequest().getPathInfo();

        MCRStoreBrowserRequest sbr = new MCRStoreBrowserRequest(pathInfo);
        Document xml = sbr.buildResponseXML();

        getLayoutService().doLayout(job.getRequest(), job.getResponse(), xml);
    }
}

class MCRStoreBrowserRequest {

    private List<String> tokens = new ArrayList<String>();

    private MCRMetadataStore store;

    MCRStoreBrowserRequest(String pathInfo) {
        StringTokenizer tokenizer = new StringTokenizer(pathInfo, "/");

        String storeID = tokenizer.nextToken();
        store = MCRStoreCenter.instance().getStore(storeID, MCRMetadataStore.class);
        if (store == null)
            store = MCRXMLMetadataManager.instance().getStore(storeID);

        while (tokenizer.hasMoreTokens())
            tokens.add(tokenizer.nextToken());
    }

    Document buildResponseXML() throws Exception {
        File dir = getRequestedDirectory();

        String[] children = dir.list();
        Element xml = new Element("storeBrowser");
        for (String child : children) {
            xml.addContent(buildXML(child));
        }

        return new Document(xml);
    }

    private Element buildXML(String child) throws Exception {
        return childIsSlot(child) ? buildSlotXML(child) : buildObjectXML(child);
    }

    private Element buildObjectXML(String child) throws IOException {
        Element xml;
        xml = new Element("object");
        xml.setAttribute("id", child);
        int id = store.slot2id(child);
        xml.setAttribute("lastModified", getLastModifiedDate(id));
        return xml;
    }

    private Element buildSlotXML(String slot) {
        Element xml;
        xml = new Element("slot");
        xml.setAttribute("from", buildMinimumIDContained(slot));
        xml.setAttribute("path", slot);
        return xml;
    }

    private String getLastModifiedDate(int id) throws IOException {
        Date lastModified = store.retrieve(id).getLastModified();
        String s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lastModified);
        return s;
    }

    private String buildMinimumIDContained(String slot) {
        StringBuffer sb = new StringBuffer();

        for (String token : tokens)
            sb.append(token);
        sb.append(slot);

        while (sb.length() < store.getIDLength())
            sb.append("0");

        int id = Integer.parseInt(sb.toString());
        id = Math.max(id, 1); // Minimum possible ID is 1, not 0
        return String.valueOf(id);
    }

    private boolean childIsSlot(String child) {
        return child.length() < store.getIDLength();
    }

    private File getRequestedDirectory() {
        File dir = getBaseDir();
        for (String token : tokens)
            dir = new File(dir, token);
        return dir;
    }

    private File getBaseDir() {
        return new File(store.getStoreConfig().getBaseDir());
    }
}
