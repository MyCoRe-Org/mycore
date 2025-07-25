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

package org.mycore.common.content.transformer;

import java.io.IOException;
import java.util.Deque;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.TransformerHandler;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.common.xsl.MCRParameterCollector;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.util.JAXBResult;

/**
 * Transforms XML content using a static XSL stylesheet.
 * The stylesheet is configured via
 * <p>
 * MCR.ContentTransformer.{ID}.Stylesheet
 * <p>
 * JAXBContext contextPath is configured via
 * <p>
 * MCR.ContentTransformer.{ID}.Context
 *
 * @author Thomas Scheffler (yagee)
 * @see JAXBContext#newInstance(String, ClassLoader)
 */
public class MCRXSL2JAXBTransformer<T> extends MCRXSLTransformer {

    private JAXBContext context;

    public MCRXSL2JAXBTransformer() {
        super();
    }

    public MCRXSL2JAXBTransformer(JAXBContext context, String... stylesheets) {
        super(stylesheets);
        this.context = context;
    }

    public MCRXSL2JAXBTransformer(JAXBContext context) {
        super();
        this.context = context;
    }

    @Override
    protected MCRContent getTransformedContent(MCRContent source, XMLReader reader,
        TransformerHandler transformerHandler) throws IOException, SAXException {
        T result;
        try {
            result = getJAXBObject(source, reader, transformerHandler);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
        return new MCRJAXBContent<>(context, result);
    }

    private T getJAXBObject(MCRContent source, XMLReader reader, TransformerHandler transformerHandler)
        throws JAXBException, IOException, SAXException {
        checkContext();
        JAXBResult result = new JAXBResult(context);
        transformerHandler.setResult(result);
        // Parse the source XML, and send the parse events to the
        // TransformerHandler.
        reader.parse(source.getInputSource());
        Object parsedResult = result.getResult();
        if (parsedResult instanceof JAXBElement<?>) {
            @SuppressWarnings("unchecked")
            JAXBElement<T> jaxbElement = (JAXBElement<T>) parsedResult;
            return jaxbElement.getValue();
        }
        @SuppressWarnings("unchecked")
        T jaxbResult = (T) result.getResult();
        return jaxbResult;
    }

    public T getJAXBObject(MCRContent source, MCRParameterCollector parameter)
        throws TransformerConfigurationException, SAXException, JAXBException, IOException,
        ParserConfigurationException {
        Deque<TransformerHandler> transformHandlers = getTransformHandlers(parameter);
        XMLReader reader = getXMLReader(transformHandlers);
        TransformerHandler lastTransformerHandler = transformHandlers.getLast();
        return getJAXBObject(source, reader, lastTransformerHandler);
    }

    public JAXBContext getContext() {
        return context;
    }

    public void setContext(JAXBContext context) {
        this.context = context;
    }

    private void checkContext() {
        Objects.requireNonNull(this.context, "No JAXBContext defined!");
    }

    @Override
    public void init(String id) {
        super.init(id);
        String property = "MCR.ContentTransformer." + id + ".Context";
        String contextPath = MCRConfiguration2.getStringOrThrow(property);
        try {
            JAXBContext context = JAXBContext.newInstance(contextPath, Thread.currentThread().getContextClassLoader());
            setContext(context);
        } catch (JAXBException e) {
            throw new MCRConfigurationException("Error while creating JAXBContext.", e);
        }
    }

}
