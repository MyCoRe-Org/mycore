package org.mycore.frontend.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class MCRParameters {

    private Map<String, List<String>> parameters = new HashMap<String, List<String>>();

    public MCRParameters() {
    }

    public MCRParameters(Map<String, String[]> input) {
        for (Entry<String, String[]> entry : input.entrySet()) {
            String parameterName = entry.getKey();
            String[] parameterValues = entry.getValue();
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

    public Map<String, String> collapse() {
        Map<String, String> collapsedMap = new HashMap<>();
        for(Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            collapsedMap.put(entry.getKey(), entry.getValue().get(0));
        }
        return collapsedMap;
    }

}
