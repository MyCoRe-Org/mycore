/*
 * 
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

package org.mycore.common.xsl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.util.IteratorIterable;
import org.mycore.common.MCRConstants;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.xml.sax.SAXException;

/**
 * Lists all *.xsl stylesheets in the web application located in any 
 * WEB-INF/lib/*.jar or WEB-INF/classes/xsl/, outputs the
 * dependencies (import/include) and contained templates.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public final class MCRXSLInfoServlet extends MCRServlet {

    private final static Logger LOGGER = LogManager.getLogger(MCRXSLInfoServlet.class);

    protected void doGetPost(MCRServletJob job) throws Exception {
        if ("true".equals(job.getRequest().getParameter("reload")))
            stylesheets.clear();

        if (stylesheets.isEmpty()) {
            LOGGER.info("Collecting stylesheet information....");
            findXSLinClassesDir();
            findXSLinLibJars();
            inspectStylesheets();
            handleUnknownStylesheets();
        }

        buildOutput(job);
    }

    private void inspectStylesheets() {
        for (Entry<String, Stylesheet> entry : stylesheets.entrySet())
            entry.getValue().inspect();
    }

    private void handleUnknownStylesheets() {
        while (!unknown.isEmpty()) {
            Set<String> list = new HashSet<String>();
            list.addAll(unknown);
            unknown.clear();

            for (String name : list) {
                Stylesheet s = new Stylesheet(name);
                stylesheets.put(name, s);
                s.inspect();
            }
        }
    }

    private void buildOutput(MCRServletJob job) throws IOException, TransformerException, SAXException {
        Element output = new Element("stylesheets");
        for (Entry<String, Stylesheet> entry : stylesheets.entrySet()) {
            Stylesheet stylesheet = entry.getValue();
            output.addContent(stylesheet.buildXML());
        }
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), new MCRJDOMContent(output));
    }

    private void findXSLinLibJars() throws IOException {
        for (String path : diveInto("/WEB-INF/lib/"))
            if (path.endsWith(".jar"))
                findXSLinJar(path);
    }

    private Set<String> diveInto(String base) {
        LOGGER.info("Diving into " + base + "...");
        Set<String> paths = getServletContext().getResourcePaths(base);

        Set<String> more = new HashSet<String>();
        more.addAll(paths);

        for (String path : paths)
            if (path.endsWith("/"))
                more.addAll(diveInto(path));

        return more;
    }

    private void findXSLinJar(String pathOfJarFile) throws IOException {
        LOGGER.info("Diving into " + pathOfJarFile + "...");

        InputStream in = getServletContext().getResourceAsStream(pathOfJarFile);
        ZipInputStream zis = new ZipInputStream(in);

        for (ZipEntry ze = null; (ze = zis.getNextEntry()) != null;) {
            String name = ze.getName();
            if (name.startsWith("xsl/") && name.endsWith(".xsl"))
                foundStylesheet(name, pathOfJarFile);
            zis.closeEntry();
        }
        zis.close();
    }

    private void findXSLinClassesDir() {
        String base = "/WEB-INF/classes/xsl/";
        for (String path : diveInto(base))
            if (path.endsWith(".xsl"))
                foundStylesheet(path, base);
    }

    private void foundStylesheet(String path, String source) {
        String file = path.substring(path.lastIndexOf("xsl/") + 4);
        LOGGER.info("Found " + file + " in " + source);
        Stylesheet stylesheet = getStylesheet(file);
        source = source.substring(9); // cut off "/WEB-INF/"
        stylesheet.origin.add(source);
    }

    private Map<String, Stylesheet> stylesheets = new HashMap<String, Stylesheet>();

    private Set<String> unknown = new HashSet<String>();

    private Stylesheet getStylesheet(String name) {
        Stylesheet stylesheet = stylesheets.get(name);
        if (stylesheet == null) {
            stylesheet = new Stylesheet(name);
            stylesheets.put(name, stylesheet);
        }
        return stylesheet;
    }

    class Stylesheet {
        String name;

        Set<String> origin = new HashSet<String>();

        Set<String> includes = new HashSet<String>();

        Set<String> imports = new HashSet<String>();

        List<Element> templates = new ArrayList<Element>();

        Element xsl;

        Stylesheet(String name) {
            this.name = name;
        }

        void inspect() {
            resolveXSL();
            if (xsl != null) {
                listTemplates();
                findIncludes("include", includes);
                findIncludes("import", imports);
            }
        }

        private void resolveXSL() {
            String uri = "resource:xsl/" + name;
            resolveXSL(uri);
            if (xsl == null) {
                resolveXSL(name);
                if (xsl != null) {
                    origin.add("URIResolver");
                }
            }
        }

        private void resolveXSL(String uri) {
            try {
                xsl = MCRURIResolver.instance().resolve(uri);
            } catch (Exception ex) {
                String msg = "Exception resolving stylesheet " + name;
                LOGGER.warn(msg, ex);
            }
        }

        private void findIncludes(String tag, Set<String> set) {
            List<Element> includes = xsl.getChildren(tag, MCRConstants.XSL_NAMESPACE);
            for (Element include : includes) {
                String href = include.getAttributeValue("href");
                LOGGER.info(name + " " + tag + "s " + href);
                set.add(href);
                if (!stylesheets.containsKey(href)) {
                    unknown.add(href);
                }
            }
        }

        private void listTemplates() {
            List<Element> list = xsl.getChildren("template", MCRConstants.XSL_NAMESPACE);
            IteratorIterable<Element> callTemplateElements = xsl
                .getDescendants(Filters.element("call-template", MCRConstants.XSL_NAMESPACE));
            LinkedList<Element> templates = new LinkedList<Element>(list);
            HashSet<String> callNames = new HashSet<String>();
            for (Element callTemplate : callTemplateElements) {
                String name = callTemplate.getAttributeValue("name");
                if (callNames.add(name)) {
                    templates.add(callTemplate);
                }
            }
            for (Element template : templates) {
                Element copy = template.clone();
                copy.removeContent();
                this.templates.add(copy);
            }
        }

        Element buildXML() {
            Element elem = new Element("stylesheet");
            elem.setAttribute("name", name);
            addValues(elem, "origin", origin);
            addValues(elem, "includes", includes);
            addValues(elem, "imports", imports);

            for (Element template : templates)
                elem.addContent(template.clone());

            return elem;
        }

        private void addValues(Element parent, String tag, Set<String> set) {
            for (String value : set)
                parent.addContent(new Element(tag).setText(value));
        }
    }
}
