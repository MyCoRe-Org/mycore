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
package org.mycore.frontend.support;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
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
 * @author René Adler (eagle)
 *
 */
public class MCRAutoDeploy implements MCRStartupHandler.AutoExecutable {

    private static final Logger LOGGER = LogManager.getLogger(MCRAutoDeploy.class);

    private static final String HANDLER_NAME = MCRAutoDeploy.class.getName();

    private static final String AUTO_DEPLOY_ATTRIB = "MCR-Auto-Deploy";

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
                .forEach(cmp -> registerWebFragment(servletContext, cmp));
        }
    }

    private void registerWebFragment(final ServletContext servletContext, final MCRComponent comp) {
        if (!isHandledByServletContainer(servletContext, comp)) {
            try {
                final JarFile jar = new JarFile(comp.getJarFile());

                Collections.list(jar.entries()).stream()
                    .filter(file -> file.getName().equals(WEB_FRAGMENT))
                    .findFirst().ifPresent(file -> {
                        try {
                            final InputStream is = jar.getInputStream(file);
                            final SAXBuilder builder = new SAXBuilder();
                            final Document doc = builder.build(is);
                            registerWebFragment(servletContext, doc);
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

    private static void registerWebFragment(final ServletContext servletContext, final Document doc) {
        final Element root = doc.getRootElement();
        final Namespace ns = root.getNamespace();

        // filters
        final List<Element> filters = root.getChildren("filter", ns);
        final List<Element> fmaps = root.getChildren("filter-mapping", ns);
        for (Element filter : filters) {
            handleFilter(servletContext, filter, ns, fmaps);
        }

        // servlets
        final List<Element> servlets = root.getChildren("servlet", ns);
        final List<Element> smaps = root.getChildren("servlet-mapping", ns);
        for (Element servlet : servlets) {
            handleServlet(servletContext, servlet, ns, smaps);
        }
    }

    private static void handleServlet(ServletContext servletContext, Element servlet, Namespace ns,
        List<Element> smaps) {
        final String name = servlet.getChildText("servlet-name", ns);
        final String className = servlet.getChildText("servlet-class", ns);

        smaps.stream()
            .filter(mapping -> mapping.getChildText("servlet-name", ns).equals(name))
            .findFirst()
            .ifPresent(mapping -> {
                handleServletMapping(servletContext, servlet, ns, mapping, name, className);
            });
    }

    private static void handleServletMapping(ServletContext servletContext, Element servlet, Namespace ns,
        Element mapping, String name, String className) {
        LOGGER.info("Register Servlet {} ({})...", name, className);
        ServletRegistration.Dynamic sr = servletContext.addServlet(name, className);
        if (sr != null) {
            for (Element url : mapping.getChildren("url-pattern", ns)) {
                LOGGER.info("...add url mapping: {}", url::getTextTrim);
                sr.addMapping(url.getTextTrim());
            }
            for (Element param : servlet.getChildren("init-param", ns)) {
                LOGGER.info("...add init-param: {}", param::getTextTrim);
                String paramName = param.getChildText("param-name", ns);
                String paramValue = param.getChildText("param-value", ns);
                sr.setInitParameter(paramName, paramValue);
            }
        } else {
            LOGGER.warn("Servlet {} already registered!", name);
        }
    }

    private static void handleFilter(ServletContext servletContext, Element filter, Namespace ns, List<Element> fmaps) {
        final String name = filter.getChildText("filter-name", ns);
        final String className = filter.getChildText("filter-class", ns);

        fmaps.stream()
            .filter(mapping -> mapping.getChildText("filter-name", ns).equals(name))
            .findFirst()
            .ifPresent(mapping -> {
                handleFilterMapping(servletContext, ns, mapping, name, className);
            });
    }

    private static void handleFilterMapping(ServletContext servletContext, Namespace ns, Element mapping, String name,
        String className) {
        LOGGER.info("Register Filter {} ({})...", name, className);
        Optional.ofNullable(servletContext.addFilter(name, className))
            .<Runnable>map(fr -> () -> {
                final List<Element> dispatchers = mapping.getChildren("dispatcher", ns);
                @SuppressWarnings("PMD.LooseCoupling")
                final EnumSet<DispatcherType> eDT = dispatchers.isEmpty() ? null : getDispatcherTypes(dispatchers);
                // servlet names
                final List<Element> servletNames = mapping.getChildren("servlet-name", ns);
                if (!servletNames.isEmpty()) {
                    fr.addMappingForServletNames(eDT, false, getServletNames(servletNames));
                }
                // url patter
                final List<Element> urlPattern = mapping.getChildren("url-pattern", ns);
                if (!urlPattern.isEmpty()) {
                    fr.addMappingForUrlPatterns(eDT, false, getUrlMapping(urlPattern));
                }
            })
            .orElse(() -> LOGGER.warn("Filter {} already registered!", name))
            .run();
    }

    private static String[] getUrlMapping(List<Element> urlPattern) {
        return urlPattern.stream()
            .map(url -> {
                LOGGER.info("...add url mapping: {}", url::getTextTrim);
                return url.getTextTrim();
            })
            .toArray(String[]::new);
    }

    private static String[] getServletNames(List<Element> servletNames) {
        return servletNames.stream()
            .map(sn -> {
                LOGGER.info("...add servlet mapping: {}",
                    sn::getTextTrim);
                return sn.getTextTrim();
            })
            .toArray(String[]::new);
    }

    @SuppressWarnings("PMD.LooseCoupling")
    private static EnumSet<DispatcherType> getDispatcherTypes(List<Element> dispatchers) {
        return dispatchers.stream()
            .map(d -> DispatcherType.valueOf(d.getTextTrim()))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(DispatcherType.class)));
    }

    @SuppressWarnings("unchecked")
    private boolean isHandledByServletContainer(ServletContext servletContext, MCRComponent comp) {
        List<String> orderedLibs = (List<String>) servletContext.getAttribute(ServletContext.ORDERED_LIBS);
        return orderedLibs.stream().anyMatch(s -> s.equals(comp.getJarFile().getName()));
    }

}
