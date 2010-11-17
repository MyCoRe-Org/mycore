package org.mycore.frontend.editor.validation.pair;

import org.mycore.frontend.editor.validation.MCRConfigurable;

public interface MCRPairValidator extends MCRConfigurable {

    public boolean isValidPair(String valueA, String valueB);

}
