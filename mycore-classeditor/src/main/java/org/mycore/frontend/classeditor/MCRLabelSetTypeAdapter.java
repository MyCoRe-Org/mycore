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

import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.DESCRIPTION;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.LANG;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.TEXT;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRJSONTypeAdapter;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.frontend.classeditor.wrapper.MCRLabelSetWrapper;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

public class MCRLabelSetTypeAdapter extends MCRJSONTypeAdapter<MCRLabelSetWrapper> {

    private static final Logger LOGGER = LogManager.getLogger(MCRLabelSetTypeAdapter.class);

    @Override
    public JsonElement serialize(MCRLabelSetWrapper labelSetWrapper, Type typeOfSrc, JsonSerializationContext context) {
        return labelsToJsonArray(labelSetWrapper.getSet());
    }

    private JsonArray labelsToJsonArray(Set<MCRLabel> labels) {
        JsonArray labelJsonArray = new JsonArray();
        for (MCRLabel label : labels) {
            JsonObject labelJsonObj = labelToJsonObj(label);
            labelJsonArray.add(labelJsonObj);
        }
        return labelJsonArray;
    }

    private JsonObject labelToJsonObj(MCRLabel label) {
        JsonObject labelJsonObj = new JsonObject();
        labelJsonObj.addProperty(LANG, label.getLang());
        labelJsonObj.addProperty(TEXT, label.getText());
        String description = label.getDescription();
        if (description != null && !"".equals(description)) {
            labelJsonObj.addProperty(DESCRIPTION, description);
        }
        return labelJsonObj;
    }

    @Override
    public MCRLabelSetWrapper deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
        Set<MCRLabel> labels = new HashSet<>();
        for (JsonElement jsonElement : json.getAsJsonArray()) {
            JsonObject labelJsonObject = jsonElement.getAsJsonObject();
            MCRLabel label = jsonLabelToMCRLabel(labelJsonObject);
            if (label != null) {
                labels.add(label);
            } else {
                LOGGER.warn("Unable to add label with empty lang or text: {}", labelJsonObject);
            }
        }
        return new MCRLabelSetWrapper(labels);
    }

    private MCRLabel jsonLabelToMCRLabel(JsonObject labelJsonObject) {
        String lang = labelJsonObject.get(LANG).getAsString();
        String text = labelJsonObject.get(TEXT).getAsString();
        JsonElement jsonElement = labelJsonObject.get(DESCRIPTION);
        String description = null;
        if (jsonElement != null) {
            description = jsonElement.getAsString();
        }
        if (lang == null || lang.trim().equals("") || text == null || text.trim().equals("")) {
            return null;
        }
        return new MCRLabel(lang, text, description);
    }
}
