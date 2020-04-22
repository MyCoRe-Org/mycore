package org.mycore.csl;

import java.io.IOException;

import org.mycore.common.config.MCRConfigurationException;

import de.undercouch.citeproc.CSL;

public class MCRCSLTransformerInstance implements AutoCloseable {

    private final AutoCloseable closeable;

    private final CSL citationProcessor;

    private final MCRItemDataProvider dataProvider;

    public MCRCSLTransformerInstance(String style, String format, AutoCloseable closeable) {
        this.closeable = closeable;
        this.dataProvider = new MCRItemDataProvider();
        try {
            this.citationProcessor = new CSL(this.dataProvider, style);
        } catch (IOException e) {
            throw new MCRConfigurationException("Error while creating CSL with Style " + style, e);
        }
        this.citationProcessor.setOutputFormat(format);

    }

    public CSL getCitationProcessor() {
        return citationProcessor;
    }

    public MCRItemDataProvider getDataProvider() {
        return dataProvider;
    }

    @Override
    public void close() throws Exception {
        this.closeable.close();
    }
}
