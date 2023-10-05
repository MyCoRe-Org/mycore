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

package org.mycore.common.content.transformer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Base64;
import java.util.Deque;
import java.util.Optional;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationBase;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRWrappedContent;
import org.mycore.common.content.streams.MCRByteArrayOutputStream;
import org.mycore.common.xml.MCREntityResolver;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.common.xml.MCRXSLTransformerUtils;
import org.mycore.common.xsl.MCRErrorListener;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.common.xsl.MCRTemplatesSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Transforms XML content using a static XSL stylesheet. The stylesheet is configured via
 * <code>MCR.ContentTransformer.{ID}.Stylesheet</code>. You may choose your own instance of
 * {@link SAXTransformerFactory} via <code>MCR.ContentTransformer.{ID}.TransformerFactoryClass</code>.
 * The default transformer factory implementation {@link org.apache.xalan.processor.TransformerFactoryImpl}
 * is configured with <code>MCR.LayoutService.TransformerFactoryClass</code>.
 *
 * @author Frank Lützenkirchen
 */
public class MCRXSLTransformer extends MCRParameterizedTransformer {

    private static final int INITIAL_BUFFER_SIZE = 32 * 1024;

    private static final MCRURIResolver URI_RESOLVER = MCRURIResolver.instance();

    private static final MCREntityResolver ENTITY_RESOLVER = MCREntityResolver.instance();

    private static Logger LOGGER = LogManager.getLogger(MCRXSLTransformer.class);

    private static MCRCache<String, MCRXSLTransformer> INSTANCE_CACHE = new MCRCache<>(100,
        "MCRXSLTransformer instance cache");

    private static long CHECK_PERIOD = MCRConfiguration2.getLong("MCR.LayoutService.LastModifiedCheckPeriod")
        .orElse(60000L);

    private static final Class<? extends TransformerFactory> DEFAULT_FACTORY_CLASS = MCRConfiguration2
        .<TransformerFactory>getClass("MCR.LayoutService.TransformerFactoryClass")
        .orElseGet(TransformerFactory.newInstance()::getClass);

    /** The compiled XSL stylesheet */
    protected MCRTemplatesSource[] templateSources;

    protected Templates[] templates;

    protected long[] modified;

    protected long modifiedChecked;

    protected SAXTransformerFactory tFactory;

    public MCRXSLTransformer(String... stylesheets) {
        this(DEFAULT_FACTORY_CLASS);
        setStylesheets(stylesheets);
    }

    public MCRXSLTransformer() {
        this(DEFAULT_FACTORY_CLASS);
    }

    public MCRXSLTransformer(Class<? extends TransformerFactory> tfClass) {
        super();
        setTransformerFactory(tfClass.getName());
    }

    /**
     * Sets the class name for {@link TransformerFactory} used by this transformer.
     *
     * Must be called for thread safety before this instance is shared to other threads.
     */
    private void setTransformerFactory(String factoryClass) throws TransformerFactoryConfigurationError {
        TransformerFactory transformerFactory = Optional.ofNullable(factoryClass)
            .map(c -> TransformerFactory.newInstance(c, MCRClassTools.getClassLoader()))
            .orElseGet(TransformerFactory::newInstance);
        LOGGER.debug("Transformerfactory: {}", transformerFactory.getClass().getName());
        transformerFactory.setURIResolver(URI_RESOLVER);
        transformerFactory.setErrorListener(MCRErrorListener.getInstance());
        if (transformerFactory.getFeature(SAXSource.FEATURE) && transformerFactory.getFeature(SAXResult.FEATURE)) {
            this.tFactory = (SAXTransformerFactory) transformerFactory;
        } else {
            throw new MCRConfigurationException("Transformer Factory " + transformerFactory.getClass().getName()
                + " does not implement SAXTransformerFactory");
        }
    }

    public static MCRXSLTransformer getInstance(String... stylesheets) {
        return getInstance(DEFAULT_FACTORY_CLASS, stylesheets);
    }

    public static MCRXSLTransformer getInstance(Class<? extends TransformerFactory> tfClass, String... stylesheets) {
        String key = tfClass.getName() + "_"
            + (stylesheets.length == 1 ? stylesheets[0] : Arrays.toString(stylesheets));
        MCRXSLTransformer instance = INSTANCE_CACHE.get(key);
        if (instance == null) {
            instance = new MCRXSLTransformer(tfClass);
            instance.setStylesheets(stylesheets);
            INSTANCE_CACHE.put(key, instance);
        }
        return instance;
    }

    @Override
    public void init(String id) {
        super.init(id);
        String property = "MCR.ContentTransformer." + id + ".Stylesheet";
        String[] stylesheets = MCRConfiguration2.getStringOrThrow(property).split(",");
        setStylesheets(stylesheets);
        MCRConfiguration2.getString("MCR.ContentTransformer." + id + ".TransformerFactoryClass")
            .ifPresent(this::setTransformerFactory);
    }

    public void setStylesheets(String... stylesheets) {
        this.templateSources = new MCRTemplatesSource[stylesheets.length];
        for (int i = 0; i < stylesheets.length; i++) {
            this.templateSources[i] = new MCRTemplatesSource(stylesheets[i].trim());
        }
        this.modified = new long[templateSources.length];
        this.modifiedChecked = 0;
        this.templates = new Templates[templateSources.length];
    }

    private void checkTemplateUptodate()
        throws TransformerConfigurationException, SAXException, ParserConfigurationException {
        boolean check = System.currentTimeMillis() - modifiedChecked > CHECK_PERIOD;
        boolean useCache = MCRConfiguration2.getBoolean("MCR.UseXSLTemplateCache").orElse(true);

        if (check || !useCache) {
            for (int i = 0; i < templateSources.length; i++) {
                long lastModified = templateSources[i].getLastModified();
                if (templates[i] == null || modified[i] < lastModified || !useCache) {
                    SAXSource source = templateSources[i].getSource();
                    templates[i] = tFactory.newTemplates(source);
                    if (templates[i] == null) {
                        throw new TransformerConfigurationException(
                            "XSLT Stylesheet could not be compiled: " + templateSources[i].getURL());
                    }
                    modified[i] = lastModified;
                }
            }
            modifiedChecked = System.currentTimeMillis();
        }
    }

    @Override
    public String getEncoding() throws TransformerException, SAXException, ParserConfigurationException {
        return getOutputProperties().getProperty("encoding", "UTF-8");
    }

    @Override
    public String getMimeType() throws TransformerException, SAXException, ParserConfigurationException {
        return getOutputProperties().getProperty("media-type", "text/xml");
    }

    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        return transform(source, new MCRParameterCollector());
    }

    @Override
    public MCRContent transform(MCRContent source, MCRParameterCollector parameter) throws IOException {
        try {
            Deque<TransformerHandler> transformHandlers = getTransformHandlers(parameter);
            XMLReader reader = getXMLReader(transformHandlers);
            TransformerHandler lastTransformerHandler = transformHandlers.getLast();
            return transform(source, reader, lastTransformerHandler, parameter);
        } catch (TransformerException | SAXException | ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void transform(MCRContent source, OutputStream out) throws IOException {
        transform(source, out, new MCRParameterCollector());
    }

    @Override
    public void transform(MCRContent source, OutputStream out, MCRParameterCollector parameter) throws IOException {
        MCRErrorListener el = null;
        try {
            Deque<TransformerHandler> transformHandlers = getTransformHandlers(parameter);
            XMLReader reader = getXMLReader(transformHandlers);
            TransformerHandler lastTransformerHandler = transformHandlers.getLast();
            el = (MCRErrorListener) lastTransformerHandler.getTransformer().getErrorListener();
            StreamResult result = new StreamResult(out);
            lastTransformerHandler.setResult(result);
            reader.parse(source.getInputSource());
        } catch (TransformerConfigurationException | SAXException | IllegalArgumentException
            | ParserConfigurationException e) {
            throw new IOException(e);
        } catch (RuntimeException e) {
            if (el != null && e.getCause() == null && el.getExceptionThrown() != null) {
                //typically if a RuntimeException has no cause,
                //we can get the "real cause" from MCRErrorListener, yeah!!!
                throw new IOException(el.getExceptionThrown());
            }
            throw e;
        }
    }

    protected MCRContent transform(MCRContent source, XMLReader reader, TransformerHandler transformerHandler,
        MCRParameterCollector parameter)
        throws IOException, SAXException, TransformerException, ParserConfigurationException {
        return new MCRTransformedContent(source, reader, transformerHandler, getLastModified(), parameter,
            getFileName(source), getMimeType(), getEncoding(), this);
    }

    protected MCRContent getTransformedContent(MCRContent source, XMLReader reader,
        TransformerHandler transformerHandler) throws IOException, SAXException {
        MCRByteArrayOutputStream baos = new MCRByteArrayOutputStream(INITIAL_BUFFER_SIZE);
        StreamResult serializer = new StreamResult(baos);
        transformerHandler.setResult(serializer);
        // Parse the source XML, and send the parse events to the
        // TransformerHandler.
        LOGGER.debug("Start transforming: {}", source.getSystemId() == null ? source.getName() : source.getSystemId());
        MCRXMLHelper.asSecureXMLReader(reader).parse(source.getInputSource());
        return new MCRByteContent(baos.getBuffer(), 0, baos.size());
    }

    private String getFileName(MCRContent content)
        throws TransformerException, SAXException, ParserConfigurationException {
        String fileName = content.getName();
        if (fileName == null) {
            return null;
        }
        //MCR-2254, ':' in fileName causes problems on Windows
        fileName = fileName.replace(':', '_');
        return FilenameUtils.removeExtension(fileName) + "." + getFileExtension();
    }

    private long getLastModified() {
        long lastModified = -1;
        for (long current : modified) {
            if (current < 0) {
                return -1;
            }
            lastModified = Math.max(lastModified, current);
        }
        return lastModified;
    }

    protected Deque<TransformerHandler> getTransformHandlers(MCRParameterCollector parameterCollector)
        throws TransformerConfigurationException, SAXException, ParserConfigurationException {
        checkTemplateUptodate();
        Deque<TransformerHandler> xslSteps = new ArrayDeque<>();
        //every transformhandler shares the same ErrorListener instance
        MCRErrorListener errorListener = MCRErrorListener.getInstance();
        for (Templates template : templates) {
            TransformerHandler handler = tFactory.newTransformerHandler(template);
            parameterCollector.setParametersTo(handler.getTransformer());
            handler.getTransformer().setErrorListener(errorListener);
            if (!xslSteps.isEmpty()) {
                Result result = new SAXResult(handler);
                xslSteps.getLast().setResult(result);
            }
            xslSteps.add(handler);
        }
        return xslSteps;
    }

    protected XMLReader getXMLReader(Deque<TransformerHandler> transformHandlerList)
        throws SAXException, ParserConfigurationException {
        XMLReader reader = MCRXMLParserFactory.getNonValidatingParser().getXMLReader();
        reader.setEntityResolver(ENTITY_RESOLVER);
        reader.setContentHandler(transformHandlerList.getFirst());
        return reader;
    }

    public Properties getOutputProperties()
        throws TransformerConfigurationException, SAXException, ParserConfigurationException {
        checkTemplateUptodate();
        Templates lastTemplate = templates[templates.length - 1];
        Properties outputProperties = lastTemplate.getOutputProperties();
        return outputProperties;
    }

    /* (non-Javadoc)
     * @see org.mycore.common.content.transformer.MCRContentTransformer#getFileExtension()
     */
    @Override
    public String getFileExtension() throws TransformerException, SAXException, ParserConfigurationException {
        String fileExtension = super.fileExtension;
        if (fileExtension != null && !getDefaultExtension().equals(fileExtension)) {
            return fileExtension;
        }
        return MCRXSLTransformerUtils.getFileExtension(getOutputProperties(), getDefaultExtension());
    }

    static class MCRTransformedContent extends MCRWrappedContent {
        private MCRContent source;

        private XMLReader reader;

        private TransformerHandler transformerHandler;

        private MCRContent transformed;

        private String eTag;

        private MCRXSLTransformer instance;

        MCRTransformedContent(MCRContent source, XMLReader reader, TransformerHandler transformerHandler,
            long transformerLastModified, MCRParameterCollector parameter, String fileName, String mimeType,
            String encoding, MCRXSLTransformer instance) throws IOException {
            this.source = source;
            this.reader = reader;
            this.transformerHandler = transformerHandler;
            LOGGER.debug("Transformer lastModified: {}", transformerLastModified);
            LOGGER.debug("Source lastModified     : {}", source.lastModified());
            this.lastModified = (transformerLastModified >= 0 && source.lastModified() >= 0)
                ? Math.max(transformerLastModified, source.lastModified())
                : -1;
            this.eTag = generateETag(lastModified, parameter.hashCode());
            this.name = fileName;
            this.mimeType = mimeType;
            this.encoding = encoding;
            this.instance = instance;
        }

        @Override
        public String getMimeType() {
            return mimeType;
        }

        @Override
        public String getName() {
            return name;
        }

        private String generateETag(final long lastModified, final int parameterHashCode) {
            //parameterHashCode is stable for this session and current request URL
            long systemLastModified = MCRConfigurationBase.getSystemLastModified();
            StringBuilder b = new StringBuilder("\"");
            byte[] unencodedETag = ByteBuffer.allocate(Long.SIZE / 4).putLong(lastModified ^ parameterHashCode)
                .putLong(systemLastModified ^ parameterHashCode).array();
            b.append(Base64.getEncoder().encodeToString(unencodedETag));
            b.append('"');
            return b.toString();
        }

        @Override
        public MCRContent getBaseContent() {
            if (transformed == null) {
                try {
                    transformed = instance.getTransformedContent(source, reader, transformerHandler);
                    transformed.setLastModified(lastModified);
                    transformed.setName(name);
                    transformed.setMimeType(mimeType);
                    transformed.setEncoding(encoding);
                } catch (IOException | SAXException e) {
                    throw new MCRException(e);
                } catch (RuntimeException e) {
                    MCRErrorListener el = (MCRErrorListener) transformerHandler.getTransformer().getErrorListener();
                    if (el != null && e.getCause() == null && el.getExceptionThrown() != null) {
                        //typically if a RuntimeException has no cause,
                        //we can get the "real cause" from MCRErrorListener, yeah!!!
                        throw new RuntimeException(MCRErrorListener.getMyMessageAndLocation(el.getExceptionThrown()),
                            el.getExceptionThrown());
                    }
                    throw e;
                } finally {
                    try {
                        transformerHandler.getTransformer().clearParameters();
                        transformerHandler.getTransformer().reset();
                    } catch (UnsupportedOperationException e) {
                        //expected and safely ignored
                    }
                }

            }
            return transformed;
        }

        @Override
        public long lastModified() {
            return lastModified;
        }

        @Override
        public String getETag() {
            return eTag;
        }

        @Override
        public boolean isUsingSession() {
            return true;
        }

        @Override
        public String getEncoding() {
            return transformed == null ? encoding : getBaseContent().getEncoding();
        }
    }
}
