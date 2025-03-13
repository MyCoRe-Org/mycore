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

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.xml.MCRURIResolver;
import org.xml.sax.SAXException;

public class MCRParameterCollectorTest extends MCRTestCase {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String XSL_RESOURCE = "xsl/copynodes.xsl";

    private static final String XALAN_FACTORY_CLASS = "org.apache.xalan.processor.TransformerFactoryImpl";

    private static final String SAXON_FACTORY_CLASS = "net.sf.saxon.TransformerFactoryImpl";

    private static final String MCR_XALAN_FACTORY_CLASS = "org.mycore.common.xsl.MCRXalanTransformerFactory";

    private static final Map<String, String> INITIAL_PROPERTIES = Map.of(
        "init1", "value1",
        "init2", "value2",
        "init3", "value3");
    private Map<String, String> randomParameters10;
    private Map<String, String> randomParameters100;
    private Map<String, String> randomParameters1000;
    private Map<String, String> randomParameters10000;

    private static void speedTest(Map<String, String> parameters, Transformer transformer) {
        MCRParameterCollector collector = new MCRParameterCollector();

        collector.setParameters(parameters);

        // clear old
        collector.setParametersTo(transformer);
        transformer.clearParameters();

        // performance check for setParametersTo
        LOGGER.info(() -> "Test time for " + parameters.size() + " parameters...");
        long startTime = System.currentTimeMillis();
        collector.setParametersTo(transformer);
        long endTime = System.currentTimeMillis() - startTime;
        LOGGER.info(() -> "Took " + endTime + "ms");
    }

    private static TransformerHandler getTransformerHandler(SAXTransformerFactory tFactory, String resourceName)
        throws TransformerConfigurationException, SAXException, ParserConfigurationException {
        MCRTemplatesSource templatesSource = new MCRTemplatesSource(resourceName);
        Templates templates = tFactory.newTemplates(templatesSource.getSource());
        return tFactory.newTransformerHandler(templates);
    }

    private static TransformerHandler getTransformerHandler(SAXTransformerFactory tFactory, Document stylesheet)
        throws TransformerConfigurationException {
        Templates templates = tFactory.newTemplates(new JDOMSource(stylesheet));
        return tFactory.newTransformerHandler(templates);
    }

    private static Document buildTestStylesheet(Map<String, String> combinedParameters) {

        Element stylesheetElement = new Element("stylesheet", MCRConstants.XSL_NAMESPACE);

        stylesheetElement.setAttribute("version", "1.0");

        combinedParameters.forEach((key, value) -> {
            Element paramElement = new Element("param", MCRConstants.XSL_NAMESPACE);
            paramElement.setAttribute("name", key);
            stylesheetElement.addContent(paramElement);
        });

        Element template = new Element("template", MCRConstants.XSL_NAMESPACE);
        template.setAttribute("match", "/root");

        Element root = new Element("root");

        combinedParameters.forEach((key, value) -> {

            Element paramElement = new Element("parameter");
            paramElement.setAttribute("name", key);

            Element valueOfElement = new Element("value-of", MCRConstants.XSL_NAMESPACE);
            valueOfElement.setAttribute("select", "$" + key);

            paramElement.addContent(valueOfElement);
            root.addContent(paramElement);
        });

        template.addContent(root);
        stylesheetElement.addContent(template);

        return new Document(stylesheetElement);
    }

    private static SAXTransformerFactory getTransformerFactory(String factoryClass) {
        TransformerFactory transformerFactory = Optional.of(factoryClass)
            .map(c -> TransformerFactory.newInstance(c, MCRClassTools.getClassLoader()))
            .orElseGet(TransformerFactory::newInstance);
        LOGGER.info("Transformer factory: {}", () -> transformerFactory.getClass().getName());
        transformerFactory.setURIResolver(MCRURIResolver.obtainInstance());
        transformerFactory.setErrorListener(new MCRErrorListener());
        if (transformerFactory.getFeature(SAXSource.FEATURE) && transformerFactory.getFeature(SAXResult.FEATURE)) {
            return (SAXTransformerFactory) transformerFactory;
        } else {
            throw new MCRConfigurationException("Transformer Factory " + transformerFactory.getClass().getName()
                + " does not implement SAXTransformerFactory");
        }
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.randomParameters10 = RandomHashMapFiller.fillWithRandomStrings(10, 8, 8);
        this.randomParameters100 = RandomHashMapFiller.fillWithRandomStrings(100, 8, 8);
        this.randomParameters1000 = RandomHashMapFiller.fillWithRandomStrings(1000, 8, 8);
        this.randomParameters10000 = RandomHashMapFiller.fillWithRandomStrings(10000, 8, 8);
    }

    @Test
    public void setParametersTo() throws TransformerException {
        SAXTransformerFactory tFactory = getTransformerFactory(SAXON_FACTORY_CLASS);
        testSetParametersTo(tFactory);

        tFactory = getTransformerFactory(XALAN_FACTORY_CLASS);
        testSetParametersTo(tFactory);

        tFactory = getTransformerFactory(MCR_XALAN_FACTORY_CLASS);
        testSetParametersTo(tFactory);
    }

    private void testSetParametersTo(SAXTransformerFactory tFactory) throws TransformerException {
        LOGGER.info("Test setParametersTo with {} implementation", tFactory.getClass().getName());
        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        Map<String, String> combinedParameters = new HashMap<>(INITIAL_PROPERTIES);
        combinedParameters.putAll(randomParameters10);

        MCRParameterCollector collector = new MCRParameterCollector();
        collector.setParameters(randomParameters10);

        Document testStylesheet = buildTestStylesheet(combinedParameters);

        LOGGER.info("Test Stylesheet: {}",
            () -> xmlOutputter.outputString(testStylesheet));

        TransformerHandler handler = getTransformerHandler(tFactory, testStylesheet);
        Transformer transformer = handler.getTransformer();
        collector.setParametersTo(transformer);

        JDOMResult outputTarget = new JDOMResult();
        JDOMSource testDocument = new JDOMSource(new Document(new Element("root")));
        transformer.transform(testDocument, outputTarget);
        Document result = outputTarget.getDocument();

        LOGGER.info("Test Result: {}", () -> xmlOutputter.outputString(result));

        Element root = result.getRootElement();

        List<Element> paramElement = root.getChildren("parameter");
        Assert.assertTrue("There should be at least 10 parameters", paramElement.size() >= 10);

        paramElement.forEach(param -> {
            String name = param.getAttributeValue("name");
            String value = param.getTextNormalize();
            Assert.assertEquals(combinedParameters.get(name), value);
        });
    }

    @Ignore
    @Test
    public void setParametersToSpeed() throws TransformerException, ParserConfigurationException, SAXException {
        SAXTransformerFactory xalanFactory = getTransformerFactory(XALAN_FACTORY_CLASS);
        TransformerHandler xalanHandler = getTransformerHandler(xalanFactory, XSL_RESOURCE);
        Transformer xalanTransformer = xalanHandler.getTransformer();

        LOGGER.info("Test xalan:");
        speedTest(randomParameters10, xalanTransformer);
        speedTest(randomParameters100, xalanTransformer);
        speedTest(randomParameters1000, xalanTransformer);
        speedTest(randomParameters10000, xalanTransformer);

        // test mcr xalan
        SAXTransformerFactory mcrXalanFactory = getTransformerFactory(MCR_XALAN_FACTORY_CLASS);
        TransformerHandler mcrXalanHandler = getTransformerHandler(mcrXalanFactory, XSL_RESOURCE);
        Transformer mcrXalanTransformer = mcrXalanHandler.getTransformer();

        LOGGER.info("Test MCR Xalan:");
        speedTest(randomParameters10, mcrXalanTransformer);
        speedTest(randomParameters100, mcrXalanTransformer);
        speedTest(randomParameters1000, mcrXalanTransformer);
        speedTest(randomParameters10000, mcrXalanTransformer);

        // test saxon
        SAXTransformerFactory saxonFactory = getTransformerFactory(SAXON_FACTORY_CLASS);
        TransformerHandler saxonHandler = getTransformerHandler(saxonFactory, XSL_RESOURCE);
        Transformer saxonTransformer = saxonHandler.getTransformer();

        LOGGER.info("Test saxon:");
        speedTest(randomParameters10, saxonTransformer);
        speedTest(randomParameters100, saxonTransformer);
        speedTest(randomParameters1000, saxonTransformer);
        speedTest(randomParameters10000, saxonTransformer);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> superProperties = super.getTestProperties();

        superProperties.putAll(INITIAL_PROPERTIES);

        return superProperties;
    }

    private static class RandomHashMapFiller {

        private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

        private static final Random RANDOM = new SecureRandom();

        public static Map<String, String> fillWithRandomStrings(int numValues, int keyLength, int valueLength) {
            HashMap<String, String> map = new HashMap<>();
            for (int i = 0; i < numValues; i++) {
                String key = generateRandomString(keyLength);
                String value = generateRandomString(valueLength);
                map.put(key, value);
            }
            return map;
        }

        private static String generateRandomString(int length) {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
            }
            return sb.toString();
        }
    }
}
