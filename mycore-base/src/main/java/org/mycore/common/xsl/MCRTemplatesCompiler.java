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

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xalan.trace.TraceManager;
import org.apache.xml.utils.WrappedRuntimeException;
import org.mycore.common.MCRExceptionCauseFinder;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.xml.MCRURIResolver;

/**
 * Compiles XSL sources, reports compile errors and returns transformer
 * instances for compiled templates.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRTemplatesCompiler {

    private static final Logger LOGGER = LogManager.getLogger(MCRTemplatesCompiler.class);

    /** The XSL transformer factory to use */
    private static SAXTransformerFactory factory;

    static {
        System.setProperty("javax.xml.transform.TransformerFactory",
            "org.apache.xalan.processor.TransformerFactoryImpl");
        TransformerFactory tf = TransformerFactory.newInstance();
        LOGGER.info("Transformerfactory: {}", tf.getClass().getName());

        if (!tf.getFeature(SAXTransformerFactory.FEATURE)) {
            throw new MCRConfigurationException("Could not load a SAXTransformerFactory for use with XSLT");
        }

        factory = (SAXTransformerFactory) tf;
        factory.setURIResolver(MCRURIResolver.instance());
        factory.setErrorListener(new ErrorListener() {
            public void error(TransformerException ex) {
                throw new WrappedRuntimeException(MCRExceptionCauseFinder.getCause(ex));
            }

            public void fatalError(TransformerException ex) {
                throw new WrappedRuntimeException(MCRExceptionCauseFinder.getCause(ex));
            }

            public void warning(TransformerException ex) {
                LOGGER.warn(ex.getMessageAndLocation());
            }
        });
    }

    /** Compiles the given XSL source code */
    public static Templates compileTemplates(MCRTemplatesSource ts) {
        try {
            Source source = ts.getSource();
            return factory.newTemplates(source);
        } catch (Exception exc) {
            LOGGER.error("Error while compiling template", exc);
            Exception cause = MCRExceptionCauseFinder.getCause(exc);
            String msg = buildErrorMessage(ts.getKey(), cause);
            throw new MCRConfigurationException(msg, cause);
        }
    }

    /** Returns a new transformer for the compiled XSL templates 
     */
    public static Transformer getTransformer(Templates templates) throws TransformerConfigurationException {
        Transformer tf = factory.newTransformerHandler(templates).getTransformer();

        // In debug mode, add a TraceListener to log stylesheet execution
        if (LOGGER.isDebugEnabled()) {
            try {
                TraceManager tm = ((org.apache.xalan.transformer.TransformerImpl) tf).getTraceManager();
                tm.addTraceListener(new MCRTraceListener());

            } catch (Exception ex) {
                LOGGER.warn(ex);
            }
        }

        return tf;
    }

    private static String buildErrorMessage(String resource, Exception cause) {
        StringBuilder msg = new StringBuilder("Error compiling XSL stylesheet ");
        msg.append(resource);

        if (cause instanceof TransformerException) {
            TransformerException tex = (TransformerException) cause;
            msg.append("\n").append(tex.getMessage());
            SourceLocator sl = tex.getLocator();
            if (sl != null) {
                msg.append(" (").append(sl.getSystemId()).append(") ");
                msg.append(" at line ").append(sl.getLineNumber());
                msg.append(" column ").append(sl.getColumnNumber());
            }
        }

        return msg.toString();
    }
}
