/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common.content.transformer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.common.xsl.MCRTemplatesSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Transforms XML content using a static XSL stylesheet.
 * The stylesheet is configured via
 * 
 * MCR.ContentTransformer.{ID}.Stylesheet

 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXSLTransformer extends MCRContentTransformer {

    private static final MCRURIResolver URI_RESOLVER = MCRURIResolver.instance();

    /** The compiled XSL stylesheet */
    protected MCRTemplatesSource[] templateSources;

    protected Templates[] templates;

    protected long[] modified;

    protected SAXTransformerFactory tFactory;

    public MCRXSLTransformer() {
        super();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setURIResolver(URI_RESOLVER);
        if (transformerFactory.getFeature(SAXSource.FEATURE) && transformerFactory.getFeature(SAXResult.FEATURE)) {
            this.tFactory = (SAXTransformerFactory) transformerFactory;
        } else {
            throw new MCRConfigurationException("Transformer Factory " + transformerFactory.getClass().getName() + " does not implement SAXTransformerFactory");
        }
    }

    @Override
    public void init(String id) {
        super.init(id);
        String property = "MCR.ContentTransformer." + id + ".Stylesheet";
        String[] stylesheets = MCRConfiguration.instance().getString(property).split(",");
        this.templateSources = new MCRTemplatesSource[stylesheets.length];
        for (int i = 0; i < stylesheets.length; i++) {
            this.templateSources[i] = new MCRTemplatesSource(stylesheets[i].trim());
        }
        this.modified = new long[templateSources.length];
        this.templates = new Templates[templateSources.length];
    }

    private void checkTemplateUptodate() throws TransformerConfigurationException, SAXException {
        for (int i = 0; i < templateSources.length; i++) {
            if (modified[i] < templateSources[i].getLastModified()) {
                templates[i] = tFactory.newTemplates(templateSources[i].getSource());
            }
        }
    }

    @Override
    public MCRContent transform(MCRContent source) throws Exception {
        checkTemplateUptodate();
        LinkedList<TransformerHandler> transformHandlerList = getTransformHandlerList();
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setEntityResolver(URI_RESOLVER);
        reader.setContentHandler(transformHandlerList.getFirst());
        TransformerHandler lastTransformerHandler = transformHandlerList.getLast();
        return transform(source, reader, lastTransformerHandler);
    }

    protected MCRContent transform(MCRContent source, XMLReader reader, TransformerHandler transformerHandler) throws IOException, SAXException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamResult serializer = new StreamResult(baos);
        transformerHandler.setResult(serializer);
        // Parse the source XML, and send the parse events to the
        // TransformerHandler.
        reader.parse(source.getInputSource());
        return new MCRByteContent(baos.toByteArray());
    }

    private LinkedList<TransformerHandler> getTransformHandlerList() throws TransformerConfigurationException {
        LinkedList<TransformerHandler> xslSteps = new LinkedList<TransformerHandler>();
        MCRParameterCollector mcrParameterCollector = new MCRParameterCollector();
        for (Templates template : templates) {
            TransformerHandler handler = tFactory.newTransformerHandler(template);
            mcrParameterCollector.setParametersTo(handler.getTransformer());
            if (!xslSteps.isEmpty()) {
                Result result = new SAXResult(handler);
                xslSteps.getLast().setResult(result);
            }
            xslSteps.add(handler);
        }
        return xslSteps;
    }

}
