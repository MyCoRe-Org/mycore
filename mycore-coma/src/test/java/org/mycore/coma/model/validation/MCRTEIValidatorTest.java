package org.mycore.coma.model.validation;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.stream.StreamSource;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

public class MCRTEIValidatorTest {

    @Test
    public void validate() throws IOException, SAXException {
        MCRTEIValidator teiValidator = getTeiValidator("xml/validTei.xml");
        teiValidator.validate();

        Assert.assertTrue(teiValidator.getErrors().size() + teiValidator.getFatals().size() == 0);
    }

    @Test
    public void validateFail() throws IOException, SAXException {
        MCRTEIValidator teiValidator = getTeiValidator("xml/invalidTei.xml");
        teiValidator.validate();

        Assert.assertTrue(teiValidator.getErrors().size() + teiValidator.getFatals().size() > 0);
    }

    private MCRTEIValidator getTeiValidator(String path) throws IOException {
        URL resource = MCRTEIValidatorTest.class.getClassLoader().getResource(path);
        return new MCRTEIValidator(new StreamSource(resource.openStream()));
    }

}
