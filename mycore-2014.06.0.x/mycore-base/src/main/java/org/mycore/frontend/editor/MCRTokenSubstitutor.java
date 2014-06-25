package org.mycore.frontend.editor;

public class MCRTokenSubstitutor {

    private MCRParameters parameters;

    public MCRTokenSubstitutor(MCRParameters parameters) {
        this.parameters = parameters;
    }

    public String substituteTokens(String text) {
        for (String name : parameters.getParameterNames()) {
            String token = buildToken(name);
            String value = parameters.getParameterValue(name);
            text = text.replace(token, value);
        }
        return text;
    }

    private String buildToken(String name) {
        return "{" + name + "}";
    }
}
