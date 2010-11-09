package org.mycore.frontend.editor;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.jdom.Element;
import org.junit.Test;
import org.junit.Before;

public class MCRAttributeTokenSubstitutorTest {

    private static final String ATTRIBUTE_VALUE_WITHOUT_ANY_TOKENS = "attribute value without any tokens";

    private static final String DEFAULT_VALUE = "defaultValue";

    private MCRAttributeTokenSubstitutor ats;

    private MCRParameters parameters;

    @Before
    public void setup() {
        Element xml = new Element("editor");
        xml.addContent(new Element("source").setAttribute("uri", "{scheme}:{id}"));
        xml.addContent(new Element("source").setAttribute("uri", "mcrobject:{id}"));
        xml.addContent(new Element("cancel").setAttribute("url", ATTRIBUTE_VALUE_WITHOUT_ANY_TOKENS));
        xml.addContent(new Element("empty"));
        
        parameters = new MCRParameters();
        parameters.addParameterValue("id", "4711");
        
        ats = new MCRAttributeTokenSubstitutor(xml, parameters);
    }

    @Test
    public void testSecondAttributeWinsBecauseFirstCannotBeCompletelySubstituted() {
        String value = ats.substituteTokens("source", "uri", DEFAULT_VALUE);
        assertThat(value, equalTo("mcrobject:4711"));
    }
    
    @Test
    public void testFirstAttributeWinsBecauseCanBeCompletelySubstituted() {
        parameters.addParameterValue("scheme", "mcrsession");
        String value = ats.substituteTokens("source", "uri", DEFAULT_VALUE);
        assertThat(value, equalTo("mcrsession:4711"));
    }

    @Test 
    public void testAttributeValueContainsNoTokensToSubstitute() {
        String value = ats.substituteTokens("cancel", "url", DEFAULT_VALUE);
        assertThat(value, equalTo(ATTRIBUTE_VALUE_WITHOUT_ANY_TOKENS));
    }

    @Test 
    public void testElementisMissingUseDefault() {
        String value = ats.substituteTokens("missingElement", "missingAttribute", DEFAULT_VALUE);
        assertThat(value, equalTo(DEFAULT_VALUE));
    }
    
    @Test
    public void testAttributeValueIsEmptyUseDefault() {
        String value = ats.substituteTokens("empty", "missingAttribute", DEFAULT_VALUE);
        assertThat(value, equalTo(DEFAULT_VALUE));
    }
}
