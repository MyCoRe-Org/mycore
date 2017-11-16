package org.mycore.wcms2.navigation;

import com.google.gson.JsonElement;

/**
 *
 * @author Matthias Eichner
 */
public interface MCRWCMSJSONProvider<O, J extends JsonElement> {

    J toJSON(O object);

    O fromJSON(J json);

}
