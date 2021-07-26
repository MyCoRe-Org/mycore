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
package org.mycore.frontend.support;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.MCRRuntimeComponentDetector;
import org.mycore.common.events.MCRStartupHandler;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;

/**
 * This StartupHandler deploys web resources and register filters/servlets to web container server,
 *  for with <code>MCR-Auto-Deploy = true</code> marked JARs.
 * 
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class MCRAutoDeploy implements MCRStartupHandler.AutoExecutable {

    private static final Logger LOGGER = LogManager.getLogger(MCRAutoDeploy.class);

    private static final String HANDLER_NAME = MCRAutoDeploy.class.getName();

    private static final String AUTO_DEPLOY_ATTRIB = "MCR-Auto-Deploy";

    private static final String RESOURCE_DIR = "META-INF/resources";

    private static final String WEB_FRAGMENT = "META-INF/web-fragment.xml";

    @Override
    public String getName() {
        return HANDLER_NAME;
    }

    @Override
    public int getPriority() {
        return 2000;
    }

    @Override
    public void startUp(final ServletContext servletContext) {
        if (servletContext != null) {
            MCRRuntimeComponentDetector.getAllComponents().stream()
                .filter(cmp -> Boolean.parseBoolean(cmp.getManifestMainAttribute(AUTO_DEPLOY_ATTRIB)))
                .forEach(cmp -> {
                    registerWebFragment(servletContext, cmp);
                    deployWebResources(servletContext, cmp);
                });
        }
    }

    private Path toNativePath(ZipEntry entry) {
        String nativePath;
        if (File.separatorChar != '/') {
            nativePath = entry.getName().replace('/', File.separatorChar);
        } else {
            nativePath = entry.getName();
        }
        return Paths.get(nativePath);
    }

    private boolean isUnzipRequired(ZipEntry entry, Path target) {
        try {
            BasicFileAttributes fileAttributes = Files.readAttributes(target, BasicFileAttributes.class);
            //entry does not contain size when read by ZipInputStream, assume equal size and just compare last modified
            return !entry.isDirectory()
                && !(fileTimeEquals(entry.getLastModifiedTime(), fileAttributes.lastModifiedTime())
                    && (entry.getSize() == -1 || entry.getSize() == fileAttributes.size()));
        } catch (IOException e) {
            LOGGER.warn("Target path {} does not exist.", target);
            return true;
        }
    }

    /**
     * compares if two file times are equal.
     *
     * Uses seconds only instead of finer granularity to compare file times of different file systems.
     */
    private boolean fileTimeEquals(FileTime a, FileTime b) {
        return a.to(TimeUnit.SECONDS) == b.to(TimeUnit.SECONDS) && a.to(TimeUnit.DAYS) == b.to(TimeUnit.DAYS);
    }

    private void deployWebResources(final ServletContext servletContext, final MCRComponent comp) {
        final Path webRoot = Optional.ofNullable(servletContext.getRealPath("/")).map(Paths::get).orElse(null);
        if (webRoot != null) {
            int resourceDirPathComponents = RESOURCE_DIR.split("/").length;
            try (InputStream fin = Files.newInputStream(comp.getJarFile().toPath());
                ZipInputStream zin = new ZipInputStream(fin)) {
                LOGGER.info("Deploy web resources from {} to {}...", comp.getName(), webRoot);
                for (ZipEntry zipEntry = zin.getNextEntry(); zipEntry != null; zipEntry = zin.getNextEntry()) {
                    if (zipEntry.getName().startsWith(RESOURCE_DIR)) {
                        Path relativePath = toNativePath(zipEntry);
                        if (relativePath.getNameCount() > resourceDirPathComponents) {
                            //strip RESOURCE_DIR:
                            relativePath = relativePath.subpath(resourceDirPathComponents, relativePath.getNameCount());
                            Path target = webRoot.resolve(relativePath);
                            if (zipEntry.isDirectory()) {
                                Files.createDirectories(target);
                            } else if (isUnzipRequired(zipEntry, target)) {
                                LOGGER.debug("...deploy {}", zipEntry.getName());
                                Files.copy(zin, target, StandardCopyOption.REPLACE_EXISTING);
                                Files.setLastModifiedTime(target, zipEntry.getLastModifiedTime());
                            }
                        }
                    }
                }
                LOGGER.info("...done.");
            } catch (final IOException e) {
                LOGGER.error("Could not deploy web resources of " + comp.getJarFile() + "!", e);
            }
        }
    }

    private void registerWebFragment(final ServletContext servletContext, final MCRComponent comp) {
        if (!isHandledByServletContainer(servletContext, comp)) {
            try {
                final JarFile jar = new JarFile(comp.getJarFile());

                Collections.list(jar.entries()).stream()
                    .filter(file -> file.getName().equals(WEB_FRAGMENT))
                    .findFirst().ifPresent(file -> {
                        final SAXBuilder builder = new SAXBuilder();
                        try {
                            final InputStream is = jar.getInputStream(file);
                            final Document doc = builder.build(is);

                            final Element root = doc.getRootElement();
                            final Namespace ns = root.getNamespace();

                            final List<Element> filters = root.getChildren("filter", ns);
                            final List<Element> fmaps = root.getChildren("filter-mapping", ns);

                            filters.forEach(filter -> {
                                final String name = filter.getChildText("filter-name", ns);
                                final String className = filter.getChildText("filter-class", ns);

                                fmaps.stream().filter(mapping -> mapping.getChildText("filter-name", ns).equals(name))
                                    .findFirst().ifPresent(mapping -> {
                                        LOGGER.info("Register Filter {} ({})...", name, className);
                                        Optional.ofNullable(servletContext.addFilter(name, className))
                                            .<Runnable>map(fr -> () -> {
                                                final List<Element> dispatchers = mapping
                                                    .getChildren("dispatcher", ns);

                                                final EnumSet<DispatcherType> eDT = dispatchers.isEmpty() ? null
                                                    : dispatchers.stream()
                                                        .map(d -> DispatcherType.valueOf(d.getTextTrim()))
                                                        .collect(Collectors
                                                            .toCollection(() -> EnumSet.noneOf(DispatcherType.class)));

                                                final List<Element> servletNames = mapping
                                                    .getChildren("servlet-name", ns);

                                                if (!servletNames.isEmpty()) {
                                                    fr.addMappingForServletNames(
                                                        eDT,
                                                        false,
                                                        servletNames.stream()
                                                            .map(sn -> {
                                                                LOGGER.info("...add servlet mapping: {}",
                                                                    sn.getTextTrim());
                                                                return sn.getTextTrim();
                                                            })
                                                            .toArray(String[]::new));
                                                }

                                                final List<Element> urlPattern = mapping
                                                    .getChildren("url-pattern", ns);

                                                if (!urlPattern.isEmpty()) {
                                                    fr.addMappingForUrlPatterns(eDT,
                                                        false, urlPattern.stream()
                                                            .map(url -> {
                                                                LOGGER.info("...add url mapping: {}",
                                                                    url.getTextTrim());
                                                                return url.getTextTrim();
                                                            })
                                                            .toArray(String[]::new));
                                                }
                                            }).orElse(() -> LOGGER.warn("Filter {} already registered!", name))
                                            .run();
                                    });
                            });

                            final List<Element> servlets = root.getChildren("servlet", ns);
                            final List<Element> smaps = root.getChildren("servlet-mapping", ns);

                            servlets.forEach(servlet -> {
                                final String name = servlet.getChildText("servlet-name", ns);
                                final String className = servlet.getChildText("servlet-class", ns);

                                smaps.stream()
                                    .filter(mapping -> mapping.getChildText("servlet-name", ns).equals(name))
                                    .findFirst()
                                    .ifPresent(mapping -> {
                                        LOGGER.info("Register Servlet {} ({})...", name, className);
                                        Optional.ofNullable(servletContext.addServlet(name, className))
                                            .<Runnable>map(sr -> () -> mapping.getChildren("url-pattern", ns).stream()
                                                .forEach(url -> {
                                                    LOGGER.info("...add url mapping: {}",
                                                        url.getTextTrim());
                                                    sr.addMapping(url.getTextTrim());
                                                }))
                                            .orElse(() -> LOGGER.error("Servlet{} already registered!", name))
                                            .run();
                                    });
                            });
                        } catch (IOException | JDOMException e) {
                            LOGGER.error("Couldn't parse " + WEB_FRAGMENT, e);
                        }
                    });

                jar.close();
            } catch (final IOException e) {
                LOGGER.error("Couldn't parse JAR!", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isHandledByServletContainer(ServletContext servletContext, MCRComponent comp) {
        List<String> orderedLibs = (List<String>) servletContext.getAttribute(ServletContext.ORDERED_LIBS);
        return orderedLibs.stream().anyMatch(s -> s.equals(comp.getJarFile().getName()));
    }

}
