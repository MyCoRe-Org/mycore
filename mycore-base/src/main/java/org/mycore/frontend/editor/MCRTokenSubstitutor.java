package org.mycore.frontend.editor;

import java.util.Map;

public class MCRTokenSubstitutor {

    private Map<String, String> variables;

    public MCRTokenSubstitutor(Map<String, String> variables) {
        this.variables = variables;
    }

    public String substituteTokens(String text) {
        for (String name : variables.keySet()) {
            String token = buildToken(name);
            String value = getValue(name, token);
            text = text.replace(token, value);
        }
        return text;
    }

    private String buildToken(String name) {
        return "{" + name + "}";
    }

    private String getValue(String name, String defaultValue) {
        String value = variables.get(name);
        return (value != null ? value : defaultValue);
    }
}
