package org.mycore.frontend.editor.validation;

public interface MCRValidator {

    public void setProperty(String name, String value);

    public String getProperty(String name);

    public boolean hasProperty(String name);
    
    public boolean hasRequiredProperties();

    public boolean isValid(Object... input);
}
