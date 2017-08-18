/**
 *
 */
package org.mycore.saxon;

import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Before;
import org.junit.Test;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.frontend.MCRLayoutUtilities;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.services.i18n.MCRTranslation;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRTransformerFactoryImplTest extends MCRTestCase {

    /* (non-Javadoc)
     * @see org.mycore.common.MCRTestCase#setUp()
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected Map<String, String> getTestProperties() {
        String staticClasses = Stream
            .of(MCRXMLFunctions.class, MCRLayoutUtilities.class, MCRTranslation.class, MCRAccessManager.class,
                MCRWebsiteWriteProtection.class, MCRTestExtensions.class)
            .map(Class::getName)
            .collect(Collectors.joining(","));
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put(MCRTransformerFactoryImpl.EXTENSION_FUNCTION_STATIC_PROPERTY, staticClasses);
        return testProperties;
    }

    /**
     * Test method for {@link org.mycore.saxon.MCRTransformerFactoryImpl#MCRTransformerFactoryImpl()}.
     * @throws TransformerException
     */
    @Test
    public void testMCRTransformerFactoryImpl() throws TransformerException {
        MCRTransformerFactoryImpl impl = new MCRTransformerFactoryImpl();
        impl.setURIResolver(MCRURIResolver.instance());
        URL testXSL = this.getClass().getClassLoader().getResource("extensionTests.xsl");

        Templates testTemplates = impl.newTemplates(new StreamSource(testXSL.toString()));
        Transformer transformer = testTemplates.newTransformer();
        transformer.transform(null, new StreamResult(System.out));
    }

}
