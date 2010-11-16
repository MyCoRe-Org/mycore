package org.mycore.frontend.editor.validation;

public interface MCRValidator extends MCRConfigurable {

    public boolean isValid(String input);

}
