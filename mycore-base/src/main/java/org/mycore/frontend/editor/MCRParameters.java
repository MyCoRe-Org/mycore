package org.mycore.frontend.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MCRParameters {

    private Map<String, List<String>> parameters;

    public MCRParameters(Map<String, String[]> input) {
        for (String parameterName : input.keySet()) {
            String[] parameterValues = input.get(parameterName);
            if (parameterValues != null)
                addParameterValues(parameterName, parameterValues);
        }
    }

    private void addParameterValues(String parameterName, String[] parameterValues) {
        for (String parameterValue : parameterValues)
            addParameterValue(parameterName, parameterValue);
    }

    public void addParameterValue(String parameterName, String parameterValue) {
        if ((parameterName == null) || (parameterValue == null))
            return;
        else if (parameterValue.isEmpty())
            return;
        else
            getOrBuildParameterValues(parameterName).add(parameterValue);
    }

    private List<String> getOrBuildParameterValues(String parameterName) {
        List<String> parameterValues = getParameterValues(parameterName);
        if (parameterValues == null) {
            parameterValues = new ArrayList<String>();
            parameters.put(parameterName, parameterValues);
        }
        return parameterValues;
    }

    public Set<String> getParameterNames() {
        return parameters.keySet();
    }

    public List<String> getParameterValues(String parameterName) {
        return parameters.get(parameterName);
    }

    public String getParameterValue(String parameterName) {
        return getParameterValue(parameterName, null);
    }

    public String getParameterValue(String parameterName, String defaultValue) {
        if (parameters.containsKey(parameterName))
            return getParameterValues(parameterName).get(0);
        else
            return defaultValue;
    }
}
