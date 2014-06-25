package org.mycore.frontend.editor;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.Before;

public class MCRParametersTest {

    private MCRParameters parameters;

    @Before
    public void buildParameters() {
        parameters = new MCRParameters();
    }

    @Test
    public void testEmptyParameters() {
        assertThat(parameters.getParameterNames(), notNullValue());
        assertTrue(parameters.getParameterNames().isEmpty());
    }

    @Test
    public void testAddValidParameter() {
        String parameterName = "id";
        String parameterValue = "4711";

        parameters.addParameterValue(parameterName, parameterValue);
        Set<String> parameterNames = parameters.getParameterNames();

        assertThat(parameterNames.size(), is(1));
        assertTrue(parameterNames.contains(parameterName));
        assertThat(parameters.getParameterValue(parameterName), equalTo(parameterValue));
    }

    @Test
    public void testAddNullParameter() {
        String parameterName = "id";
        parameters.addParameterValue(parameterName, null);
        assertTrue(parameters.getParameterNames().isEmpty());
        assertThat(parameters.getParameterValue(parameterName), nullValue());
    }

    @Test
    public void testAddEmptyParameter() {
        String parameterName = "id";
        parameters.addParameterValue(parameterName, "");
        assertTrue(parameters.getParameterNames().isEmpty());
        assertThat(parameters.getParameterValue(parameterName), nullValue());
    }

    @Test
    public void testTwoParameters() {
        String parameterName1 = "id";
        String parameterValue1 = "4711";
        parameters.addParameterValue(parameterName1, parameterValue1);
        String parameterName2 = "type";
        String parameterValue2 = "foo";
        parameters.addParameterValue(parameterName2, parameterValue2);
        Set<String> parameterNames = parameters.getParameterNames();
        assertThat(parameterNames.size(), is(2));
        assertTrue(parameterNames.contains(parameterName1));
        assertTrue(parameterNames.contains(parameterName2));
        assertThat(parameters.getParameterValue(parameterName1), equalTo(parameterValue1));
        assertThat(parameters.getParameterValue(parameterName2), equalTo(parameterValue2));
    }

    @Test
    public void testDuplicateParameter() {
        String parameterName = "id";
        String firstValue = "4711";
        String secondValue = "4812";
        parameters.addParameterValue(parameterName, firstValue);
        parameters.addParameterValue(parameterName, secondValue);

        Set<String> parameterNames = parameters.getParameterNames();
        List<String> parameterValues = parameters.getParameterValues(parameterName);

        assertThat(parameterNames.size(), is(1));
        assertTrue(parameterNames.contains(parameterName));
        assertThat(parameters.getParameterValue(parameterName), equalTo(firstValue));
        assertThat(parameterValues.size(), is(2));
        assertThat(parameterValues.get(0), equalTo(firstValue));
        assertThat(parameterValues.get(1), equalTo(secondValue));
    }

    @Test
    public void testMissingParameter() {
        assertThat(parameters.getParameterValue("parameterName"), nullValue());
    }

    @Test
    public void testDefault() {
        String defaultValue = "defaultValue";
        assertThat(parameters.getParameterValue("parameterName", defaultValue), equalTo(defaultValue));
    }

    @Test
    public void testCreateFromMap() {
        Map<String, String[]> map = new HashMap<String, String[]>();
        map.put("one", new String[] { "1" });
        map.put("two", new String[] { "2a", "2b" });
        map.put("emptyArray", new String[] {} );
        map.put("emptyValue", new String[] {""});
        map.put("nullValue", null );

        MCRParameters parameters = new MCRParameters(map);
        
        assertThat(parameters.getParameterValues("one").size(),equalTo(1));
        assertThat(parameters.getParameterValues("two").size(),equalTo(2));
        assertThat(parameters.getParameterValue("emptyArray"),nullValue());
        assertThat(parameters.getParameterValue("emptyValue"),nullValue());
        assertThat(parameters.getParameterValue("nullValue"),nullValue());
    }
}
