/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.classeditor;

import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.CATEGID;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.ROOTID;

import java.lang.reflect.Type;

import org.mycore.common.MCRJSONTypeAdapter;
import org.mycore.datamodel.classifications2.MCRCategoryID;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

public class MCRCategoryIDTypeAdapter extends MCRJSONTypeAdapter<MCRCategoryID> {

    @Override
    public MCRCategoryID deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
        JsonObject idJsonObj = json.getAsJsonObject();
        JsonElement rootIDObj = idJsonObj.get(ROOTID);
        JsonElement categIDObj = idJsonObj.get(CATEGID);

        String rootID = rootIDObj.getAsString();
        String categID = categIDObj == null ? "" : categIDObj.getAsString();
        return new MCRCategoryID(rootID, categID);
    }

    @Override
    public JsonElement serialize(MCRCategoryID id, Type typeOfSrc, JsonSerializationContext context) {
        String rootID = id.getRootID();
        String categID = id.getID();

        JsonObject idJsonObj = new JsonObject();
        idJsonObj.addProperty(ROOTID, rootID);
        if (categID != null && !"".equals(categID)) {
            idJsonObj.addProperty(CATEGID, categID);
        }

        return idJsonObj;
    }
}
