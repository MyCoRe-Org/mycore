/*
 * $RCSfile$
 * $Revision$ $Date$
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

package org.mycore.frontend.editor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;

public class MCREditorDefReader {

    private final static Logger logger = Logger.getLogger(MCREditorDefReader.class);

    /**
     * Reads the editor definition from the given URI
     * 
     * @param validate
     *            if true, validate editor definition against schema
     */
    static Element readDef(String uri, String ref, boolean validate) {
        Element editor = new Element("editor");
        editor.setAttribute("id", ref);
        editor.addContent(resolveInclude(uri, ref, true).getIncludedElements());

        if (logger.isDebugEnabled()) {
            XMLOutputter xout = new XMLOutputter();
            xout.setFormat(Format.getPrettyFormat());
            logger.debug("--------------------");
            logger.debug("\n" + xout.outputString(editor) + "\n");
            logger.debug("--------------------");
        }

        if (validate) {
            Document doc = new Document(editor);
            Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            editor.setAttribute("noNamespaceSchemaLocation", "editor.xsd", xsi);
            editor.setAttribute("id", ref);

            XMLOutputter xout = new XMLOutputter();
            xout.setFormat(Format.getPrettyFormat());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                xout.output(doc, baos);
                baos.close();
            } catch (IOException ex) {
                throw new MCRException("Exception while validating editor definition", ex);
            }

            logger.info("Validating editor definition against XML schema...");
            MCRXMLHelper.parseXML(baos.toByteArray(), true);
            logger.info("Editor definition successfully validated.");

            editor.detach();
            editor.removeAttribute("noNamespaceSchemaLocation", xsi);
        }

        return editor;
    }

    /**
     * Returns that direct or indirect child element of the given element, thats
     * ID attribute has the given value.
     * 
     * @param id
     *            the value the ID attribute must have
     * @param parent
     *            the element to start searching with
     * @return the child element that has the given ID, or null if no such
     *         element exists.
     */
    static Element findElementByID(String id, Element parent) {
        List children = parent.getChildren();

        for (int i = 0; i < children.size(); i++) {
            Element child = (Element) (children.get(i));

            if (id.equals(child.getAttributeValue("id"))) {
                return child;
            }
            Element found = findElementByID(id, child);

            if (found != null) {
                return found;
            }
        }

        return null;
    }

    /**
     * A cache of reusable resolved includes. Key is URI and IDREF, cached value
     * is a container element that holds the resolved includes.
     */
    protected static MCRCache includesCache = new MCRCache(100);

    /**
     * Resolves the uri and idref to a list of elements to include. If idref is
     * null or empty, the root element at the given URI is used and its children
     * are returned as includes. If idref is not empty, the direct or indirect
     * child element of the element at URI is looked up, and its children are
     * used. All contained includes are resolved recursively, so the returned
     * result is include-free.
     * 
     * @param uri
     *            the URI where to get the elements from
     * @param idref
     *            if not null, include contents of element with that ID
     * @return a List of resolved, included elements
     */
    protected static MCRResolvedInclude resolveInclude(String uri, String idref, boolean cacheable) {
        if (idref == null) {
            idref = "";
        }

        // May be the included resource is already in the cache
        String key = idref + "@" + uri;

        MCREditorServlet.logger.debug("Editor resolving include " + key);

        Element cached = (Element) (includesCache.get(key));

        if (cached != null) {
            MCREditorServlet.logger.debug("Editor resolved include from cache: " + key);

            return new MCRResolvedInclude(cached, cacheable);
        }

        // Get the elements to include from uri
        Element container = MCRURIResolver.instance().resolve(uri);

        // If idref is given, include contents of element with that id
        if (idref.length() > 0) {
            container = findElementByID(idref, container);
        }

        // Recursively resolve include elements in the included resource
        cacheable = resolveIncludes(container) && cacheable;
        MCREditorServlet.logger.debug("Editor resolved include " + key + " is cacheable? " + cacheable);

        if (cacheable) {
            includesCache.put(key, container);
        }

        return new MCRResolvedInclude(container, cacheable);
    }

    /**
     * Recursively removes include elements that are direct or indirect children
     * of the given container element and replaces them with the included
     * resource. Includes that may be contained in included resources are
     * recursively resolved, too.
     * 
     * @param container
     *            The element where to start resolving includes
     */
    protected static boolean resolveIncludes(Element container) {
        boolean allCacheable = true;
        List children = container.getChildren();

        for (int i = 0; i < children.size(); i++) {
            Element child = (Element) (children.get(i));

            if (child.getName().equals("include")) {
                String ref = child.getAttributeValue("ref");
                String uri = child.getAttributeValue("uri");

                if ((uri == null) || (uri.trim().length() == 0)) {
                    continue;
                }

                boolean includeCacheable = !"false".equals(child.getAttributeValue("cacheable"));
                allCacheable = allCacheable && includeCacheable;

                children.remove(child);

                MCRResolvedInclude ri = resolveInclude(uri, ref, includeCacheable);
                children.addAll(i--, ri.getIncludedElements());
                allCacheable = allCacheable && ri.isCacheable();
            } else {
                allCacheable = allCacheable & resolveIncludes(child);
            }
        }

        return allCacheable;
    }
}

class MCRResolvedInclude {
    private List included;

    private boolean cacheable;

    MCRResolvedInclude(Element container, boolean cacheable) {
        this.cacheable = cacheable;
        this.included = new java.util.Vector();

        List children = container.getChildren();

        for (int i = 0; i < children.size(); i++) {
            Element child = (Element) (children.get(i));
            included.add(child.clone());
        }
    }

    boolean isCacheable() {
        return cacheable;
    }

    List getIncludedElements() {
        return included;
    }
}
