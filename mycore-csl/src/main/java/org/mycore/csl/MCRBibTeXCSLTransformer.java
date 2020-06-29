package org.mycore.csl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStringContent;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.xsl.MCRParameterCollector;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.output.Bibliography;

public class MCRBibTeXCSLTransformer extends MCRParameterizedTransformer {

    public static final String DEFAULT_FORMAT = "text";

    public static final String DEFAULT_STYLE = "nature";
    private static final String CONFIG_PREFIX = "MCR.ContentTransformer.";

    private String configuredFormat;

    private String configuredStyle;

    private final Map<String, Stack<MCRCSLTransformerInstance>> transformerInstances;

    {
        transformerInstances = new HashMap<>();
    }

    @Override
    public void init(String id) {
        super.init(id);
        configuredFormat = MCRConfiguration2.getString(CONFIG_PREFIX + id + ".format").orElse(DEFAULT_FORMAT);
        configuredStyle = MCRConfiguration2.getString(CONFIG_PREFIX + id + ".style").orElse(DEFAULT_STYLE);
    }

    @Override public MCRContent transform(MCRContent source) throws IOException {
        return null;
    }

    private MCRCSLTransformerInstance getTransformerInstance(String style, String format) {
        synchronized (transformerInstances) {
            if (getStyleFormatTransformerStack(style, format).size() > 0) {
                return transformerInstances.get(mapKey(style, format)).pop();
            }
        }

        AtomicReference<MCRCSLTransformerInstance> instance = new AtomicReference<>();
        final MCRCSLTransformerInstance newInstance = new MCRCSLTransformerInstance(style, format,
            () -> returnTransformerInstance(instance.get(), style, format));
        instance.set(newInstance);
        return newInstance;
    }

    private Stack<MCRCSLTransformerInstance> getStyleFormatTransformerStack(String style, String format) {
        return transformerInstances.computeIfAbsent(mapKey(style, format), (a) -> new Stack<>());
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
            final Stack<MCRCSLTransformerInstance> styleFormatTransformerStack = getStyleFormatTransformerStack(style,
                format);
            if (!styleFormatTransformerStack.contains(instance)) {
                styleFormatTransformerStack.push(instance);
            }
        }
    }

    @Override public MCRContent transform(MCRContent bibtext, MCRParameterCollector parameter) throws IOException {
        final String format = parameter != null ? parameter.getParameter("format", configuredFormat) : configuredFormat;
        final String style = parameter != null ? parameter.getParameter("style", configuredStyle) : configuredStyle;
        try (MCRCSLTransformerInstance transformerInstance = getTransformerInstance(style, format)) {
            final MCRItemDataProvider dataProvider = transformerInstance.getDataProvider();
            final CSL citationProcessor = transformerInstance.getCitationProcessor();

            dataProvider.addBibTeX(bibtext);
            dataProvider.registerCitationItems(citationProcessor);
            Bibliography biblio = citationProcessor.makeBibliography();
            String result = biblio.makeString();

            return new MCRStringContent(result);
        } catch (Exception e) {
            throw new MCRException("Error while returning CSL instance to pool!", e);
        }
    }
}
