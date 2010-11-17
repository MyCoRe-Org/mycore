package org.mycore.frontend.editor.validation.value;

import org.mycore.frontend.editor.validation.MCRConfigurable;

public interface MCRValidator extends MCRConfigurable {

    public boolean isValid(String input);

}
