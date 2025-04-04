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

package org.mycore.common.xml;

import java.io.File;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.SAXOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;

/**
 * This class implements XSLTransformation functions to be used in all other
 * MyCoRe packages. The class is implemented as singleton and should be very
 * easy to use. So here is an example:
 * 
 * <PRE>
 * // Get an instance of the class 
 * MCRXSLTransformation transformation = MCRXSLTransformation.getInstance(); 
 * // Get the template: myStylesheet could be a String (i.e. a filename), 
 * // a File or a StreamSource Templates
 * templates = transformation.getStylesheet(myStylesheet); 
 * // Next, you are in need of a TransformerHandler: 
 * TransformerHandler th = transformation.getTransformerHandler(templates); 
 * // Now you are able to set some properties (if you want!): 
 * Properties parameters = new Properties(); ...
 * transformation.setParameters(th, parameters); 
 * // Finally, you need an OutputStream and might get at work: 
 * OutputStream out = response.getOutputStream(); 
 * transformation.transform(jdom, th, out); 
 * // You might also want to transform into something different, perhaps a ZIP-File:
 * OutputStream out = new ZipOutputStream(response.getOutputStream());
 * ((ZipOutputStream) out).setLevel(Deflater.BEST_COMPRESSION); 
 * ZipEntry ze = new ZipEntry("_index.htm"); 
 * ((ZipOutputStream) out).putNextEntry(ze); ... 
 * // After all this work is done, you could close the OutputStream: 
 * out.close(); 
 * // This is not done by <CODE>transform</CODE>, the later example 
 * // should show, why. * 
 * </PRE>
 * 
 * @author Werner Gresshoff
 * 
 * @deprecated use {@link org.mycore.common.xsl.MCRXSLTransformerFactory} or
 * {@link org.mycore.common.content.transformer.MCRXSLTransformer} instead
 */
@Deprecated
public class MCRXSLTransformation {
    private static final Logger LOGGER = LogManager.getLogger();

    private static SAXTransformerFactory saxFactory;

    private static TransformerFactory factory = TransformerFactory.newInstance();

    private static final Map EMPTY_PARAMETERS = Collections.unmodifiableMap(new HashMap<>(0, 1));

    static {
        factory.setURIResolver(MCRURIResolver.obtainInstance());
        try {
            TransformerFactory tf = TransformerFactory.newInstance();

            if (tf.getFeature(SAXTransformerFactory.FEATURE)) {
                saxFactory = (SAXTransformerFactory) tf;
                saxFactory.setURIResolver(MCRURIResolver.obtainInstance());
            } else {
                LOGGER.fatal("TransformerFactory could not be initialized.");
            }
        } catch (TransformerFactoryConfigurationError tfce) {
            if (tfce.getMessage() != null) {
                LOGGER.fatal(tfce.getMessage());
            } else {
                LOGGER.fatal("Error in TranformerFactory configuration.");
            }
        }
    }

    /**
     * @deprecated Use {@link #obtainInstance()} instead
     */
    @Deprecated
    public static synchronized MCRXSLTransformation getInstance() {
        return obtainInstance();
    }

    /**
     * Method getInstance. Creates an instance of MCRXSLTransformation, when
     * called the first time.
     * 
     * @return MCRXSLTransformation
     */
    public static MCRXSLTransformation obtainInstance() {
        return LazyInstanceHolder.SHARED_INSTANCE;
    }

    /**
     * Method getStylesheet. Returns a precompiled stylesheet.
     * 
     * @param stylesheet
     *            Full path to the stylesheet
     * @return Templates The precompiled Stylesheet
     */
    public Templates getStylesheet(String stylesheet) {
        File styleFile = new File(stylesheet);

        if (!styleFile.exists()) {
            LOGGER.fatal("The Stylesheet doesn't exist: {}", stylesheet);

            return null;
        }

        return getStylesheet(styleFile);
    }

    /**
     * Method getStylesheet. Returns a precompiled stylesheet.
     * 
     * @param stylesheet
     *            A File with the stylesheet code
     * @return Templates The precompiled Stylesheet
     */
    public Templates getStylesheet(File stylesheet) {
        return getStylesheet(new StreamSource(stylesheet));
    }

    /**
     * Method getStylesheet. Returns a precompiled stylesheet.
     * 
     * @param stylesheet
     *            A StreamSource
     * @return Templates The precompiled Stylesheet
     */
    public Templates getStylesheet(Source stylesheet) {
        try {

            return saxFactory.newTemplates(stylesheet);
        } catch (TransformerConfigurationException tcx) {
            LOGGER.fatal(tcx.getMessageAndLocation());

            return null;
        }
    }

    /**
     * Method getTransformerHandler. Returns a TransformerHandler for the given
     * Template.
     * 
     * @return TransformerHandler
     */
    public TransformerHandler getTransformerHandler(Templates stylesheet) {
        try {

            return saxFactory.newTransformerHandler(stylesheet);
        } catch (TransformerConfigurationException tcx) {
            LOGGER.fatal(tcx.getMessageAndLocation());

            return null;
        }
    }

    /**
     * Method setParameters. Set some parameters which can be used by the
     * Stylesheet for the transformation.
     * 
     */
    public static void setParameters(TransformerHandler handler, Map parameters) {
        setParameters(handler.getTransformer(), parameters);
    }

    /**
     * Method setParameters. Set some parameters which can be used by the
     * Stylesheet for the transformation.
     * 
     */
    public static void setParameters(Transformer transformer, Map parameters) {
        for (Object o : parameters.keySet()) {
            String name = o.toString();
            String value = parameters.get(name).toString();
            transformer.setParameter(name, value);
        }
    }

    /**
     * Method transform. Transforms a JDOM-Document to the given OutputStream
     * 
     */
    public void transform(Document in, TransformerHandler handler, OutputStream out) {
        handler.setResult(new StreamResult(out));

        try {
            new SAXOutputter(handler).output(in);
        } catch (JDOMException ex) {
            LOGGER.error("Error while transforming an XML document with an XSL stylesheet.");
        }
    }

    /**
     * Method transform. Transforms a JDOM-Document <i>in </i> with a given
     * <i>stylesheet </i> to a new document.
     * 
     * @param in
     *            A JDOM-Document.
     * @param stylesheet
     *            The Filename with complete path (this is not a servlet!) of
     *            the stylesheet.
     * @return Document The new document or null, if an exception was thrown.
     */
    public static Document transform(Document in, String stylesheet) {
        return transform(in, stylesheet, EMPTY_PARAMETERS);
    }

    /**
     * Method transform. Transforms a JDOM-Document <i>in </i> with a given
     * <i>stylesheet </i> to a new document.
     * 
     * @param in
     *            A JDOM-Document.
     * @param stylesheet
     *            The Filename with complete path (this is not a servlet!) of
     *            the stylesheet.
     * @param parameters
     *            parameters used by the stylesheet for transformation
     * @return Document The new document or null, if an exception was thrown.
     */
    public static Document transform(Document in, String stylesheet, Map parameters) {
        return transform(in, new StreamSource(new File(stylesheet)), parameters);
    }

    /**
     * Method transform. Transforms a JDOM-Document <i>in </i> with a given
     * <i>stylesheet </i> to a new document.
     * 
     * @param in
     *            A JDOM-Document.
     * @param stylesheet
     *            The Filename with complete path (this is not a servlet!) of
     *            the stylesheet.
     * @param parameters
     *            parameters used by the stylesheet for transformation
     * @return Document The new document or null, if an exception was thrown.
     */
    public static Document transform(Document in, Source stylesheet, Map parameters) {
        try {
            Transformer transformer = factory.newTransformer(stylesheet);
            setParameters(transformer, parameters);
            return transform(in, transformer);
        } catch (TransformerException e) {
            LOGGER.fatal(e.getMessage(), e);
            return null;
        }
    }

    /**
     * transforms a jdom Document via XSLT.
     * 
     * @param in Document input
     * @param transformer Transformer handling the transformation process
     * @return the transformation result as jdom Document
     * @throws TransformerException if transformation fails
     */
    public static Document transform(Document in, Transformer transformer) throws TransformerException {
        JDOMResult out = new JDOMResult();
        transformer.transform(new JDOMSource(in), out);
        return out.getDocument();
    }

    private static final class LazyInstanceHolder {
        public static final MCRXSLTransformation SHARED_INSTANCE = new MCRXSLTransformation();
    }

}
