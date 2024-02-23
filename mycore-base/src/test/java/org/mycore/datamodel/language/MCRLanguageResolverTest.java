package org.mycore.datamodel.language;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

import javax.xml.transform.TransformerException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class MCRLanguageResolverTest extends MCRTestCase {

    @Test
    public void resolve() throws TransformerException {
        // german code
        MCRLanguageResolver languageResolver = new MCRLanguageResolver();
        JDOMSource jdomSource = (JDOMSource) languageResolver.resolve("language:de", "");
        Document document = jdomSource.getDocument();
        assertNotNull(document);
        assertNotNull(document.getRootElement());
        Element languageElement = document.getRootElement();
        assertEquals(2, languageElement.getChildren().size());

        // empty code
        assertThrows(IllegalArgumentException.class, () -> {
            languageResolver.resolve("language:", "");
        });
    }

}
