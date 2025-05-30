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

package org.mycore.csl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStringContent;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.xsl.MCRParameterCollector;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.output.Bibliography;

public class MCRCSLTransformer extends MCRParameterizedTransformer {

    public static final String DEFAULT_FORMAT = "text";

    public static final String DEFAULT_STYLE = "nature";

    public static final String ITEM_PROVIDER = "ItemProviderClass";

    private static final String CONFIG_PREFIX = "MCR.ContentTransformer.";

    private final Map<String, Deque<MCRCSLTransformerInstance>> transformerInstances = new ConcurrentHashMap<>();

    private String configuredFormat;

    private String configuredStyle;

    private String configuredItemProviderProperty;

    private boolean unsorted;

    @Override
    public void init(String id) {
        super.init(id);
        configuredFormat = MCRConfiguration2.getString(CONFIG_PREFIX + id + ".format").orElse(DEFAULT_FORMAT);
        configuredStyle = MCRConfiguration2.getString(CONFIG_PREFIX + id + ".style").orElse(DEFAULT_STYLE);
        configuredItemProviderProperty = CONFIG_PREFIX + id + "." + ITEM_PROVIDER;
        // when set to true, then the sorting of the CSL Style is used instead
        // of the one provided by the ItemDataProvider
        unsorted = !MCRConfiguration2.getBoolean(CONFIG_PREFIX + id + ".CSLSorting").orElse(false);
        createItemDataProvider();
    }

    private MCRItemDataProvider createItemDataProvider() {
        return MCRConfiguration2.getInstanceOfOrThrow(MCRItemDataProvider.class, configuredItemProviderProperty);
    }

    @Override
    public MCRContent transform(MCRContent source) {
        return null;
    }

    private MCRCSLTransformerInstance getTransformerInstance(String style, String format) {
        synchronized (transformerInstances) {
            if (!getStyleFormatTransformerStack(style, format).isEmpty()) {
                return transformerInstances.get(mapKey(style, format)).pop();
            }
        }

        AtomicReference<MCRCSLTransformerInstance> instance = new AtomicReference<>();
        final MCRCSLTransformerInstance newInstance = new MCRCSLTransformerInstance(style, format,
            () -> returnTransformerInstance(instance.get(), style, format), createItemDataProvider());
        instance.set(newInstance);
        return newInstance;
    }

    private Deque<MCRCSLTransformerInstance> getStyleFormatTransformerStack(String style, String format) {
        return transformerInstances.computeIfAbsent(mapKey(style, format), (a) -> new ArrayDeque<>());
    }

    private String mapKey(String style, String format) {
        return style + "_" + format;
    }

    private void returnTransformerInstance(MCRCSLTransformerInstance instance, String style, String format) {
        try {
            instance.getCitationProcessor().reset();
            instance.getDataProvider().reset();
        } catch (RuntimeException e) {
            // if a error happens the instances may be not reset, so we trow away the instance
            return;
        }
        synchronized (transformerInstances) {
            final Deque<MCRCSLTransformerInstance> styleFormatTransformerStack = getStyleFormatTransformerStack(style,
                format);
            if (!styleFormatTransformerStack.contains(instance)) {
                styleFormatTransformerStack.push(instance);
            }
        }
    }

    @Override
    public MCRContent transform(MCRContent bibtext, MCRParameterCollector parameter) {
        final String format = parameter != null ? parameter.getParameter("format", configuredFormat) : configuredFormat;
        final String style = parameter != null ? parameter.getParameter("style", configuredStyle) : configuredStyle;
        try (MCRCSLTransformerInstance transformerInstance = getTransformerInstance(style, format)) {
            final CSL citationProcessor = transformerInstance.getCitationProcessor();
            final MCRItemDataProvider dataProvider = transformerInstance.getDataProvider();

            dataProvider.addContent(bibtext);
            citationProcessor.registerCitationItems(dataProvider.getIds(), unsorted);
            Bibliography biblio = citationProcessor.makeBibliography();
            String result = biblio.makeString();

            return new MCRStringContent(result);
        } catch (Exception e) {
            throw new MCRException("Error while returning CSL instance to pool!", e);
        }
    }
}
