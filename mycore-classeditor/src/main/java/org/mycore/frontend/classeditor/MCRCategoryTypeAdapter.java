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

import static org.mycore.frontend.classeditor.json.MCRJSONCategoryHelper.PROP_CHILDREN;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryHelper.PROP_HAS_LINK;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryHelper.PROP_ID;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryHelper.PROP_LABELS;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryHelper.PROP_PARENTID;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryHelper.PROP_POSITION;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryHelper.PROP_URISTR;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mycore.common.MCRJSONTypeAdapter;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.frontend.classeditor.json.MCRJSONCategory;
import org.mycore.frontend.classeditor.wrapper.MCRCategoryListWrapper;
import org.mycore.frontend.classeditor.wrapper.MCRLabelSetWrapper;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

public class MCRCategoryTypeAdapter extends MCRJSONTypeAdapter<MCRJSONCategory> {
    private MCRCategLinkService linkService;

    @Override
    public MCRJSONCategory deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
        JsonObject categJsonObject = json.getAsJsonObject();
        MCRJSONCategory deserializedCateg = new MCRJSONCategory();

        JsonElement idJsonElement = categJsonObject.get(PROP_ID);
        if (idJsonElement != null) {
            MCRCategoryID id = context.deserialize(idJsonElement, MCRCategoryID.class);
            deserializedCateg.setId(id);
        }

        JsonElement parentIdJsonElement = categJsonObject.get(PROP_PARENTID);
        if (parentIdJsonElement != null) {
            MCRCategoryID parentId = context.deserialize(parentIdJsonElement, MCRCategoryID.class);
            deserializedCateg.setParentID(parentId);
        }

        JsonElement positionJsonElem = categJsonObject.get(PROP_POSITION);
        if (positionJsonElem != null) {
            deserializedCateg.setPositionInParent(positionJsonElem.getAsInt());
        }

        JsonElement labelSetWrapperElem = categJsonObject.get(PROP_LABELS);
        if (labelSetWrapperElem != null) {
            MCRLabelSetWrapper labelSetWrapper = context.deserialize(labelSetWrapperElem, MCRLabelSetWrapper.class);
            deserializedCateg.setLabels(labelSetWrapper.getSet());
        }

        JsonElement uriJsonElement = categJsonObject.get(PROP_URISTR);
        if (uriJsonElement != null) {
            String uriStr = uriJsonElement.getAsString();
            deserializedCateg.setURI(URI.create(uriStr));
        }

        return deserializedCateg;
    }

    @Override
    public JsonElement serialize(MCRJSONCategory category, Type arg1, JsonSerializationContext contextSerialization) {
        JsonObject rubricJsonObject = new JsonObject();
        MCRCategoryID id = category.getId();
        if (id != null) {
            rubricJsonObject.add(PROP_ID, contextSerialization.serialize(id));
        }

        Set<MCRLabel> labels = category.getLabels();
        rubricJsonObject.add(PROP_LABELS, contextSerialization.serialize(new MCRLabelSetWrapper(labels)));
        URI uri = category.getURI();
        if (uri != null) {
            rubricJsonObject.addProperty(PROP_URISTR, uri.toString());
        }

        if (category.hasChildren()) {
            List<MCRCategory> children = category.getChildren();
            Map<MCRCategoryID, Boolean> linkMap = getLinkService().hasLinks(category);
            if (linkMap.values().contains(true)) {
                rubricJsonObject.addProperty(PROP_HAS_LINK, true);
            }
            rubricJsonObject.add(PROP_CHILDREN,
                contextSerialization.serialize(new MCRCategoryListWrapper(children, linkMap)));
        }

        return rubricJsonObject;
    }

    private MCRCategLinkService getLinkService() {
        if (linkService == null) {
            try {
                linkService = MCRConfiguration.instance().getInstanceOf("Category.Link.Service");
            } catch (MCRConfigurationException e) {
                linkService = MCRCategLinkServiceFactory.getInstance();
            }
        }

        return linkService;
    }
}
