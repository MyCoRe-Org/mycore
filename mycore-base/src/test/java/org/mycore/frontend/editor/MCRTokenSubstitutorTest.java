package org.mycore.frontend.editor;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRTokenSubstitutorTest {

    private MCRTokenSubstitutor tokenSubstitutor;
    
    @Before
    public void before()
    {
      MCRParameters parameters = new MCRParameters();
      parameters.addParameterValue("id", "4711");
      parameters.addParameterValue("type", "mcrobject");
      tokenSubstitutor = new MCRTokenSubstitutor(parameters);
    }
    
    @Test
    public void testSubstitutions() {
        String output = tokenSubstitutor.substituteTokens("{type}:{id}");
        assertThat(output, equalTo("mcrobject:4711"));
    }
    
    @Test
    public void testMissingParameter() {
        String text = "Missing token: {missing}";
        String output = tokenSubstitutor.substituteTokens(text);
        assertThat( output, equalTo( text ));
    }

    @Test
    public void testNoTokens() {
        String text = "This text does not contain any tokens.";
        String output = tokenSubstitutor.substituteTokens(text);
        assertThat( output, equalTo( text ));
    }

    @Test
    public void testMultipleOccurrences() {
        String text = "The ID {id} is the same as {id}";
        String output = tokenSubstitutor.substituteTokens(text);
        assertThat( output, equalTo( "The ID 4711 is the same as 4711" ));
    }
}
