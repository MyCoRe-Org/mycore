package org.mycore.multitenancy.wcms.navigation;

import com.google.gson.JsonElement;

/**
 *
 * @author Matthias Eichner
 * @param <O>
 * @param <J>
 */
public interface JSONProvider<O, J extends JsonElement> {

    public J toJSON(O object);

    public O fromJSON(J json);

}
