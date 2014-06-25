package org.mycore.wcms2.navigation;

import com.google.gson.JsonElement;

/**
 *
 * @author Matthias Eichner
 * @param <O>
 * @param <J>
 */
public interface MCRWCMSJSONProvider<O, J extends JsonElement> {

    public J toJSON(O object);

    public O fromJSON(J json);

}
