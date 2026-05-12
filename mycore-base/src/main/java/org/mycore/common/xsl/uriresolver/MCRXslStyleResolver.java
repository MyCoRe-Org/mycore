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

package org.mycore.common.xsl.uriresolver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.common.xsl.MCRParameterCollector;

/**
 * Transforms the result of another resolver using one or more XSL stylesheets.
 * <p>
 * Usage:
 * <pre>
 * xslStyle:&lt;stylesheet&gt;&lt;,&lt;stylesheet&gt;&gt;&lt;?param1=value1&amp;param2=value2&gt;&lt;#flavor&gt;:&lt;anyMyCoReURI&gt;
 * </pre>
 */
public class MCRXslStyleResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CONFIG_PREFIX = "MCR.URIResolver.XSLStyle.Flavor.";

    private static final String FLAVOR_PARAMETER = "xslStyleFlavor";

    private final Flavor defaultFlavor;

    private final Map<String, Flavor> flavors;

    public MCRXslStyleResolver() {
        Class<? extends TransformerFactory> defaultFactoryClass = MCRConfiguration2
            .<TransformerFactory>getClass("MCR.LayoutService.TransformerFactoryClass")
            .orElseGet(TransformerFactory.newInstance()::getClass);
        String defaultXslFolder = MCRConfiguration2
            .getStringOrThrow(MCRURIResolver.PROPERTY_XSL_FOLDER);

        defaultFlavor = new Flavor(defaultFactoryClass, defaultXslFolder);
        LOGGER.info("Working with default flavor {}", defaultFlavor);

        flavors = new HashMap<>();
        for (String flavorName : getFlavorNames()) {
            String factoryClassProperty = CONFIG_PREFIX + flavorName + ".TransformerFactoryClass";
            Class<? extends TransformerFactory> factoryClass = MCRConfiguration2
                .<TransformerFactory>getClass(factoryClassProperty)
                .orElseThrow(() -> MCRConfiguration2.createConfigurationException(factoryClassProperty));

            String xslFolderProperty = CONFIG_PREFIX + flavorName + ".XSLFolder";
            String xslFolder = MCRConfiguration2.getStringOrThrow(xslFolderProperty);

            Flavor flavor = new Flavor(factoryClass, xslFolder);
            LOGGER.info("Working with {} flavor {}", flavorName, flavor);
            flavors.put(flavorName, flavor);
        }
    }

    private static Set<String> getFlavorNames() {
        return MCRConfiguration2
            .getSubPropertiesMap(CONFIG_PREFIX)
            .keySet()
            .stream()
            .map(key -> key.substring(0, key.indexOf('.')))
            .collect(Collectors.toSet());
    }

    /**
     * Resolves the target URI and transforms its content using the specified XSL stylesheets.
     * <p>Multiple stylesheets are applied in order. Optional query parameters are passed to the
     * transformer. An optional flavor selects the transformer factory and XSL folder to use;
     * if omitted, the default flavor is used. The flavor can also be passed as the
     * {@code xslStyleFlavor} query parameter.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{stylesheet}[,{stylesheet}...][?param=value...][#{flavor}]:{anyMCRUri}
     * </pre>
     * <p>Example request:
     * <pre>
     *   xslStyle:my-stylesheet?lang=de:mcrobject:mcr_document_00000001
     *   xslStyle:sheet1,sheet2?lang=de#xslt3:mcrobject:mcr_document_00000001
     * </pre>
     * <p>Example response: the transformed XML content produced by the stylesheets.
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet, passed through to the delegated resolver
     * @return a {@link Source} wrapping the transformed content
     * @throws TransformerException if the target URI cannot be resolved or the transformation fails
     * @throws MCRUsageException if the target URI is missing or an unknown flavor is specified
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String help = href.substring(href.indexOf(':') + 1);

        // check if target URI is present
        int configurationEnd = help.indexOf(':');
        if (configurationEnd == -1) {
            throw new MCRUsageException("Target URI missing in " + href);
        }

        //  copy target URI from end of href, ensure that resolved element won't be null
        int targetStart = configurationEnd + 1;
        String targetUri = help.substring(targetStart);
        if (!targetUri.startsWith("notnull:")) {
            targetUri = "notnull:" + targetUri;
        }

        //  copy flavor from end of href, if present
        String flavorName = "";
        Flavor flavor = defaultFlavor;
        int flavorNameStart = help.lastIndexOf('#', configurationEnd);
        if (flavorNameStart != -1) {
            flavorName = help.substring(flavorNameStart + 1, configurationEnd);
            configurationEnd = flavorNameStart;
        }

        if (!flavorName.isEmpty()) {
            flavor = flavors.get(flavorName);
            if (flavor == null) {
                throw new MCRUsageException("Unknown flavor " + flavorName + " in " + href);
            }
        }

        //  copy parameters from end of href, if present
        String parameters = "";
        int paramsStart = help.lastIndexOf('?', configurationEnd);
        if (paramsStart != -1) {
            parameters = help.substring(paramsStart + 1, configurationEnd);
            configurationEnd = paramsStart;
        }

        Map<String, String> parameterMap = MCRURIResolver.getParameterMap(parameters);
        String flavorParameter = parameterMap.remove(FLAVOR_PARAMETER);

        //  copy stylesheets from href
        String stylesheetPaths = help.substring(0, configurationEnd);

        // resolve target URI
        Source resolved = MCRURIResolver.obtainInstance().resolve(targetUri, base);
        assert resolved != null;

        try {
            if (resolved.getSystemId() == null) {
                resolved.setSystemId(targetUri);
            }

            if (flavorName.isEmpty() && flavorParameter != null && !flavorParameter.isBlank()) {
                flavorName = flavorParameter;
                flavor = flavors.get(flavorName);
                if (flavor == null) {
                    throw new MCRUsageException("Unknown flavor " + flavorName + " in " + href);
                }
            }

            // prepare transformer
            String[] stylesheets = augmentStylesheetsPaths(stylesheetPaths.split(","),
                flavor.xslFolder);
            MCRXSLTransformer transformer = MCRXSLTransformer.obtainInstance(flavor.transformerFactory,
                stylesheets);

            //prepare parameter collector
            MCRParameterCollector parameterCollector = MCRParameterCollector.ofCurrentSession();
            parameterCollector.setParameters(parameterMap);

            // perform transformation
            MCRSourceContent content = new MCRSourceContent(resolved);
            return transformer.transform(content, parameterCollector).getSource();
        } catch (IOException e) {
            MCRURIResolver.findAndThrowTransformerException(e);
            throw new TransformerException(e);
        }
    }

    private String[] augmentStylesheetsPaths(String[] stylesheets, String xslFolder) {
        for (int i = 0; i < stylesheets.length; i++) {
            stylesheets[i] = xslFolder + "/" + stylesheets[i] + ".xsl";
        }
        return stylesheets;
    }

    private record Flavor(Class<? extends TransformerFactory> transformerFactory, String xslFolder) {
    }

}
