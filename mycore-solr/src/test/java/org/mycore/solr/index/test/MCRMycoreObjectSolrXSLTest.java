package org.mycore.solr.index.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPathFactory;
import org.junit.Test;

public class MCRMycoreObjectSolrXSLTest {

    @Test
    public void singleTransform()
        throws JDOMException, IOException, TransformerFactoryConfigurationError, TransformerException {
        String testFilePath = "/" + getClass().getSimpleName() + "/oneObj.xml";
        InputStream testXMLAsStream = getClass().getResourceAsStream(testFilePath);

        JDOMResult jdomResult = xslTransformation(testXMLAsStream);

        Document resultXML = jdomResult.getDocument();

        //        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        //        xmlOutputter.output(resultXML, System.out);

        List<Element> mycoreojectTags = XPathFactory.instance()
            .compile("/solr-document-container/source/mycoreobject", Filters.element()).evaluate(resultXML);
        assertEquals(1, mycoreojectTags.size());

        List<Element> userFieldTags = XPathFactory.instance()
            .compile("/solr-document-container/source/user", Filters.element()).evaluate(resultXML);
        assertEquals(1, userFieldTags.size());
    }

    @Test
    public void multiTransform()
        throws JDOMException, IOException, TransformerFactoryConfigurationError, TransformerException {
        String testFilePath = "/" + getClass().getSimpleName() + "/multiplObj.xml";
        InputStream testXMLAsStream = getClass().getResourceAsStream(testFilePath);

        JDOMResult jdomResult = xslTransformation(testXMLAsStream);

        Document resultXML = jdomResult.getDocument();

        //        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        //        xmlOutputter.output(resultXML, System.out);

        List<Element> mycoreojectTags = XPathFactory.instance()
            .compile("/solr-document-container/source/mycoreobject", Filters.element()).evaluate(resultXML);
        assertEquals(3, mycoreojectTags.size());

        List<Element> userFieldTags = XPathFactory.instance()
            .compile("/solr-document-container/source/user", Filters.element()).evaluate(resultXML);
        assertEquals(3, userFieldTags.size());
    }

    @Test
    public void derivates()
        throws JDOMException, IOException, TransformerFactoryConfigurationError, TransformerException {
        String testFilePath = "/" + getClass().getSimpleName() + "/xml/derivateObj.xml";
        InputStream testXMLAsStream = getClass().getResourceAsStream(testFilePath);

        //        JDOMResult jdomResult = xslTransformation(testXMLAsStream, "/" + getClass().getSimpleName() + "/xsl/mcr2solrOld.xsl");
        JDOMResult jdomResult = xslTransformation(testXMLAsStream);

        Document resultXML = jdomResult.getDocument();

        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        xmlOutputter.output(resultXML, System.out);

        List<Element> mycoreojectTags = XPathFactory.instance()
            .compile("/solr-document-container/source/mycorederivate", Filters.element()).evaluate(resultXML);
        assertEquals(1, mycoreojectTags.size());
    }

    private JDOMResult xslTransformation(InputStream testXMLAsStream)
        throws TransformerFactoryConfigurationError, TransformerException {
        return xslTransformation(testXMLAsStream, "/xsl/mycoreobject-solr.xsl");
    }

    private JDOMResult xslTransformation(InputStream testXMLAsStream, String styleSheetPath)
        throws TransformerConfigurationException,
        TransformerFactoryConfigurationError, TransformerException {
        InputStream stylesheetAsStream = getClass().getResourceAsStream(styleSheetPath);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        URIResolver mockIncludeResolver = new MockURIResolver();
        transformerFactory.setURIResolver(mockIncludeResolver);
        Templates templates = transformerFactory.newTemplates(new StreamSource(stylesheetAsStream));
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
            String testFilePath = "/" + MCRMycoreObjectSolrXSLTest.class.getSimpleName()
                + "/mockClassification.xml";
            InputStream testXMLAsStream = getClass().getResourceAsStream(testFilePath);
            return new StreamSource(testXMLAsStream);
        }

    }

    public class MockURIResolver implements URIResolver {

        @Override
        public Source resolve(String arg0, String arg1) throws TransformerException {
            return new JDOMSource(new Element("empty"));
        }

    }

}
