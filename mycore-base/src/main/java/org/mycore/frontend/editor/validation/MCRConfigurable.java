package org.mycore.frontend.editor.validation;

public interface MCRConfigurable {

    public void setProperty(String name, String value);

    public String getProperty(String name);

    public boolean hasProperty(String name);
    
    public boolean hasRequiredProperties();

}
