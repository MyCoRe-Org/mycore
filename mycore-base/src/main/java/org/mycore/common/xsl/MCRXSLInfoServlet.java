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

package org.mycore.common.xsl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.util.IteratorIterable;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.xml.sax.SAXException;

/**
 * Lists all *.xsl stylesheets in the web application located in any 
 * WEB-INF/lib/*.jar or WEB-INF/classes/xsl/ or in {@link MCRConfigurationDir}, outputs the
 * dependencies (import/include) and contained templates.
 * 
 * @author Frank Lützenkirchen
 */
public final class MCRXSLInfoServlet extends MCRServlet {

    private static final Logger LOGGER = LogManager.getLogger(MCRXSLInfoServlet.class);

    private final Map<String, Stylesheet> stylesheets = new HashMap<>();

    private final Set<String> unknown = new HashSet<>();

    protected void doGetPost(MCRServletJob job) throws Exception {
        if ("true".equals(job.getRequest().getParameter("reload"))) {
            stylesheets.clear();
        }

        if (stylesheets.isEmpty()) {
            LOGGER.info("Collecting stylesheet information....");
            findInConfigDir();
            findXSLinClassesDir();
            findXSLinLibJars();
            inspectStylesheets();
            handleUnknownStylesheets();
        }

        buildOutput(job);
    }

    private void findInConfigDir() throws IOException {
        final File configDir = MCRConfigurationDir.getConfigurationDirectory();
        if (configDir == null) {
            return;
        }
        findInConfigResourcesDir(configDir);
        findInConfigLibDir(configDir);
    }

    private void findInConfigResourcesDir(File configDir) throws IOException {
        final Path resources = configDir.toPath().resolve("resources");
        final Path xslResourcesPath = resources.resolve("xsl");
        doForFilesRecursive(xslResourcesPath, n -> n.endsWith(".xsl"),
            file -> foundStylesheet(resources.toUri().relativize(file.toUri()).toString(),
                configDir.toPath().relativize(file).toString()));
    }

    private void findInConfigLibDir(File configDir) throws IOException {
        final Path lib = configDir.toPath().resolve("lib");
        try {
            doForFilesRecursive(lib, n -> n.endsWith(".jar"),
                file -> {
                    try (InputStream in = Files.newInputStream(file)) {
                        findInJarInputStream(configDir.toPath().relativize(file).toString(), in);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        } catch (UncheckedIOException uioe) {
            throw uioe.getCause();
        }
    }

    private void doForFilesRecursive(Path baseDir, Predicate<String> lowerCaseCheck, Consumer<Path> pathConsumer)
        throws IOException {
        if (Files.isDirectory(baseDir)) {
            Files.walkFileTree(baseDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (lowerCaseCheck.test(file.getFileName().toString().toLowerCase(Locale.ROOT))) {
                        pathConsumer.accept(file);
                    }
                    return super.visitFile(file, attrs);
                }
            });
        }
    }

    private void inspectStylesheets() {
        for (Entry<String, Stylesheet> entry : stylesheets.entrySet()) {
            entry.getValue().inspect();
        }
    }

    private void handleUnknownStylesheets() {
        while (!unknown.isEmpty()) {
            Set<String> list = new HashSet<>(unknown);
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
        for (String path : diveInto("/WEB-INF/lib/")) {
            if (path.endsWith(".jar")) {
                findXSLinJar(path);
            }
        }
    }

    private Set<String> diveInto(String base) {
        LOGGER.info("Diving into {}...", base);
        Set<String> paths = getServletContext().getResourcePaths(base);

        Set<String> more = new HashSet<>();
        if (paths != null) {
            more.addAll(paths);

            for (String path : paths) {
                if (path.endsWith("/")) {
                    more.addAll(diveInto(path));
                }
            }
        }

        return more;
    }

    private void findXSLinJar(String pathOfJarFile) throws IOException {
        LOGGER.info("Diving into {}...", pathOfJarFile);

        try (InputStream in = getServletContext().getResourceAsStream(pathOfJarFile)) {
            findInJarInputStream(pathOfJarFile, in);
        }
    }

    private void findInJarInputStream(String pathOfJarFile, InputStream in) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(in)) {

            for (ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry()) {
                String name = ze.getName();
                if (name.startsWith("xsl/") && name.endsWith(".xsl")) {
                    foundStylesheet(name, pathOfJarFile);
                }
                zis.closeEntry();
            }
        }
    }

    private void findXSLinClassesDir() {
        String base = "/WEB-INF/classes/xsl/";
        for (String path : diveInto(base)) {
            if (path.endsWith(".xsl")) {
                foundStylesheet(path, base);
            }
        }
    }

    private void foundStylesheet(String path, String source) {
        String file = path.substring(path.lastIndexOf("xsl/") + 4);
        LOGGER.info("Found {} in {}", file, source);
        Stylesheet stylesheet = getStylesheet(file);
        if (source.startsWith("/WEB-INF/")) {
            source = source.substring("/WEB-INF/".length()); // cut off
        }
        stylesheet.origin.add(source);
    }

    private Stylesheet getStylesheet(String name) {
        return stylesheets.computeIfAbsent(name, Stylesheet::new);
    }

    class Stylesheet {
        String name;

        Set<String> origin = new HashSet<>();

        Set<String> includes = new HashSet<>();

        Set<String> imports = new HashSet<>();

        List<Element> templates = new ArrayList<>();

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
                LOGGER.info("{} {}s {}", name, tag, href);
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
            List<Element> templates = new ArrayList<>(list);
            HashSet<String> callNames = new HashSet<>();
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

            for (Element template : templates) {
                elem.addContent(template.clone());
            }

            return elem;
        }

        private void addValues(Element parent, String tag, Set<String> set) {
            for (String value : set) {
                parent.addContent(new Element(tag).setText(value));
            }
        }
    }
}
