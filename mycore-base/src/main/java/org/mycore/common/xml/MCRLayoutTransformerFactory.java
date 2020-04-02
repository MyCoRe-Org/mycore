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

package org.mycore.common.xml;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.content.transformer.MCRIdentityTransformer;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;

/**
 * This class acts as a {@link MCRContentTransformer} factory for {@link MCRLayoutService}.
 * @author Thomas Scheffler (yagee)
 * @author Sebastian Hofmann
 */
public class MCRLayoutTransformerFactory {
    /** Map of transformer instances by ID */
    private static Map<String, MCRContentTransformer> transformers = new ConcurrentHashMap<>();

    private static Logger LOGGER = LogManager.getLogger(MCRLayoutTransformerFactory.class);

    protected static final MCRIdentityTransformer NOOP_TRANSFORMER = new MCRIdentityTransformer("text/xml", "xml");

    /**
     * Returns the transformer with the given ID. If the transformer is not instantiated yet,
     * it is created and initialized.
     */
    public MCRContentTransformer getTransformer(String id) {
        return transformers.computeIfAbsent(id, (transformerID) -> {
            try {
                Optional<MCRContentTransformer> configuredTransformer = getConfiguredTransformer(id);
                if (configuredTransformer.isPresent()) {
                    return configuredTransformer.get();
                }
                return buildLayoutTransformer(id);
            } catch (Exception e) {
                throw new MCRException("Error while creating Transformer!", e);
            }
        });
    }

    protected Optional<MCRContentTransformer> getConfiguredTransformer(String id) {
        return Optional.ofNullable(MCRContentTransformerFactory.getTransformer(id.replaceAll("-default$", "")));
    }

    private MCRContentTransformer buildLayoutTransformer(String id)
        throws ParserConfigurationException, TransformerException, SAXException {
        String idStripped = id.replaceAll("-default$", "");
        LOGGER.debug("Configure property MCR.ContentTransformer.{}.Class if you do not want to use default behaviour.",
            idStripped);
        String stylesheet = getResourceName(id);
        if (stylesheet == null) {
            LOGGER.debug("Using noop transformer for {}", idStripped);
            return NOOP_TRANSFORMER;
        }
        String[] stylesheets = getStylesheets(idStripped, stylesheet);
        MCRContentTransformer transformer = MCRXSLTransformer.getInstance(stylesheets);
        LOGGER.debug("Using stylesheet '{}' for {}", Lists.newArrayList(stylesheets), idStripped);
        return transformer;
    }

    protected String[] getStylesheets(String id, String stylesheet)
        throws TransformerException, SAXException, ParserConfigurationException {
        List<String> ignore = MCRConfiguration2.getString("MCR.LayoutTransformerFactory.Default.Ignore")
            .map(MCRConfiguration2::splitValue)
            .map(s1 -> s1.collect(Collectors.toList()))
            .orElseGet(Collections::emptyList);
        List<String> defaults = Collections.emptyList();
        if (!ignore.contains(id)) {
            MCRXSLTransformer transformerTest = MCRXSLTransformer.getInstance(stylesheet);
            String outputMethod = transformerTest.getOutputProperties().getProperty(OutputKeys.METHOD, "xml");
            if (isXMLOutput(outputMethod, transformerTest)) {
                defaults = MCRConfiguration2.getString("MCR.LayoutTransformerFactory.Default.Stylesheets")
                    .map(MCRConfiguration2::splitValue)
                    .map(s -> s.collect(Collectors.toList()))
                    .orElseGet(Collections::emptyList);
            }
        }
        String[] stylesheets = new String[1 + defaults.size()];
        stylesheets[0] = stylesheet;
        for (int i = 0; i < defaults.size(); i++) {
            stylesheets[i + 1] = defaults.get(i);
        }
        return stylesheets;
    }

    protected boolean isXMLOutput(String outputMethod, MCRXSLTransformer transformerTest)
        throws ParserConfigurationException, TransformerException, SAXException {
        return "xml".equals(outputMethod);
    }

    private String getResourceName(String id) {
        LOGGER.debug("MCRLayoutService using style {}", id);

        String styleName = buildStylesheetName(id);
        try {
            if (MCRXMLResource.instance().exists(styleName, MCRClassTools.getClassLoader())) {
                return styleName;
            }
        } catch (Exception e) {
            throw new MCRException("Error while loading stylesheet: " + styleName, e);
        }

        // If no stylesheet exists, forward raw xml instead
        // You can transform raw xml code by providing a stylesheed named
        // [doctype]-xml.xsl now
        if (id.endsWith("-xml") || id.endsWith("-default")) {
            LOGGER.warn("XSL stylesheet not found: {}", styleName);
            return null;
        }
        throw new MCRException("XSL stylesheet not found: " + styleName);
    }

    /**
     * Builds the filename of the stylesheet to use, e. g. "playlist-simple.xsl"
     */
    private String buildStylesheetName(String id) {
        return String.format(Locale.ROOT, "xsl/%s.xsl", id.replaceAll("-default$", ""));
    }

}
