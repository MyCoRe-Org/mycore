package org.mycore.solr.index.test;

import static org.junit.Assert.*;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.xpath.XPath;
import org.junit.Test;

public class MCRMycoreObjectSolrXSLTest {

    @Test
    public void singleTransform() throws JDOMException, IOException, TransformerFactoryConfigurationError, TransformerException {
        String testFilePath = File.separator + getClass().getSimpleName() + File.separator + "oneObj.xml";
        InputStream testXMLAsStream = getClass().getResourceAsStream(testFilePath);
        
        JDOMResult jdomResult = xslTransformation(testXMLAsStream);
        
        Document resultXML = jdomResult.getDocument();
        
//        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
//        xmlOutputter.output(resultXML, System.out);
        
        List mycoreojectTags = XPath.selectNodes(resultXML, "/solr-document-container/source/mycoreobject");
        assertEquals(1, mycoreojectTags.size());
        
        List userFieldTags = XPath.selectNodes(resultXML, "/solr-document-container/source/user/field");
        assertEquals(12, userFieldTags.size());
        
        List userTopFieldTags = XPath.selectNodes(resultXML, "/solr-document-container/source/user/field[contains(@name, '.top')]");
        assertEquals(4, userTopFieldTags.size());
    }
    
    @Test
    public void multiTransform() throws JDOMException, IOException, TransformerFactoryConfigurationError, TransformerException {
        String testFilePath = File.separator + getClass().getSimpleName() + File.separator + "multiplObj.xml";
        InputStream testXMLAsStream = getClass().getResourceAsStream(testFilePath);
        
        JDOMResult jdomResult = xslTransformation(testXMLAsStream);
        
        Document resultXML = jdomResult.getDocument();
        
//        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
//        xmlOutputter.output(resultXML, System.out);
        
        List mycoreojectTags = XPath.selectNodes(resultXML, "/solr-document-container/source/mycoreobject");
        assertEquals(3, mycoreojectTags.size());
        
        List userFieldTags = XPath.selectNodes(resultXML, "/solr-document-container/source/user/field");
        assertEquals(36, userFieldTags.size());
        
        List userTopFieldTags = XPath.selectNodes(resultXML, "/solr-document-container/source/user/field[contains(@name, '.top')]");
        assertEquals(12, userTopFieldTags.size());
    }
    
    private JDOMResult xslTransformation(InputStream testXMLAsStream) throws TransformerConfigurationException,
            TransformerFactoryConfigurationError, TransformerException {
        String styleSheetPath = MessageFormat.format("{0}xsl{0}mycoreobject-solr.xsl", File.separator);
        InputStream stylesheetAsStream = getClass().getResourceAsStream(styleSheetPath);
        
        Templates templates = TransformerFactory.newInstance().newTemplates(new StreamSource(stylesheetAsStream));
        Transformer transformer = templates.newTransformer();
        URIResolver resolver = new MockResolver();
        transformer.setURIResolver(resolver);
        JDOMResult jdomResult = new JDOMResult();
        transformer.transform(new StreamSource(testXMLAsStream), jdomResult);
        return jdomResult;
    }
    
    private class MockResolver implements URIResolver {

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            String testFilePath = File.separator + MCRMycoreObjectSolrXSLTest.class.getSimpleName() + File.separator + "mockClassification.xml";
            InputStream testXMLAsStream = getClass().getResourceAsStream(testFilePath);
            return new StreamSource(testXMLAsStream);
        }

    }

}
