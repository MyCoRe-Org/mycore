package org.mycore.frontend.editor.validation;

public interface MCRConfigurable {

    public void setProperty(String name, String value);

    public boolean hasRequiredProperties();

}
