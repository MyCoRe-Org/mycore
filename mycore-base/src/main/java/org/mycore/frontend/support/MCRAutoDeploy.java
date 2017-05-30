/*
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
package org.mycore.frontend.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;

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

    private void deployWebResources(final ServletContext servletContext, final MCRComponent comp) {
        final String webRoot = servletContext.getRealPath("/");
        if (webRoot != null) {
            try {
                final JarFile jar = new JarFile(comp.getJarFile());

                LOGGER.info("Deploy web resources to " + webRoot + "...");
                Collections.list(jar.entries()).stream().filter(file -> file.getName().startsWith(RESOURCE_DIR))
                    .forEach(file -> {
                        final String fileName = file.getName().substring(RESOURCE_DIR.length());
                        LOGGER.debug("...deploy " + fileName);

                        final File f = new File(webRoot + File.separator + fileName);
                        if (file.isDirectory()) {
                            f.mkdir();
                        } else {
                            try {
                                final InputStream is = jar.getInputStream(file);
                                final FileOutputStream fos = new FileOutputStream(f);
                                while (is.available() > 0) {
                                    fos.write(is.read());
                                }
                                fos.close();
                            } catch (IOException e) {
                                LOGGER.error("Couldn't deploy file " + fileName + ".", e);
                            }
                        }
                    });
                LOGGER.info("...done.");

                jar.close();
            } catch (final IOException e) {
                LOGGER.error("Couldn't parse JAR!", e);
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
                                        LOGGER.info("Register Filter " + name + " (" + className + ")...");
                                        Optional.ofNullable(servletContext.addFilter(name, className))
                                            .<Runnable> map(fr -> () -> {
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
                                                                LOGGER.info(
                                                                    "...add servlet mapping: " + sn.getTextTrim());
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
                                                                LOGGER.info("...add url mapping: " + url.getTextTrim());
                                                                return url.getTextTrim();
                                                            })
                                                            .toArray(String[]::new));
                                                }
                                            }).orElse(() -> LOGGER
                                                .warn("Filter " + name + " already registered!"))
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
                                        LOGGER.info("Register Servlet " + name + " (" + className + ")...");
                                        Optional.ofNullable(servletContext.addServlet(name, className))
                                            .<Runnable> map(sr -> () -> {
                                                mapping.getChildren("url-pattern", ns).stream()
                                                    .forEach(url -> {
                                                        LOGGER.info("...add url mapping: " + url.getTextTrim());
                                                        sr.addMapping(url.getTextTrim());
                                                    });
                                            }).orElse(() -> LOGGER
                                                .error("Servlet" + name + " already registered!"))
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
