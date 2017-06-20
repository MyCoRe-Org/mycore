package org.mycore.frontend.classeditor;

import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.CHILDREN;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.HASLINK;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.ID;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.LABELS;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.PARENTID;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.POSITION;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.URISTR;

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

        JsonElement idJsonElement = categJsonObject.get(ID);
        if (idJsonElement != null) {
            MCRCategoryID id = context.deserialize(idJsonElement, MCRCategoryID.class);
            deserializedCateg.setId(id);
        }

        JsonElement parentIdJsonElement = categJsonObject.get(PARENTID);
        if (parentIdJsonElement != null) {
            MCRCategoryID parentId = context.deserialize(parentIdJsonElement, MCRCategoryID.class);
            deserializedCateg.setParentID(parentId);
        }

        JsonElement positionJsonElem = categJsonObject.get(POSITION);
        if (positionJsonElem != null) {
            deserializedCateg.setPositionInParent(positionJsonElem.getAsInt());
        }

        JsonElement labelSetWrapperElem = categJsonObject.get(LABELS);
        if (labelSetWrapperElem != null) {
            MCRLabelSetWrapper labelSetWrapper = context.deserialize(labelSetWrapperElem, MCRLabelSetWrapper.class);
            deserializedCateg.setLabels(labelSetWrapper.getSet());
        }

        JsonElement uriJsonElement = categJsonObject.get(URISTR);
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
            rubricJsonObject.add(ID, contextSerialization.serialize(id));
        }

        Set<MCRLabel> labels = category.getLabels();
        rubricJsonObject.add(LABELS, contextSerialization.serialize(new MCRLabelSetWrapper(labels)));
        URI uri = category.getURI();
        if (uri != null) {
            rubricJsonObject.addProperty(URISTR, uri.toString());
        }

        if (category.hasChildren()) {
            List<MCRCategory> children = category.getChildren();
            Map<MCRCategoryID, Boolean> linkMap = getLinkService().hasLinks(category);
            if (linkMap.values().contains(true)) {
                rubricJsonObject.addProperty(HASLINK, true);
            }
            rubricJsonObject.add(CHILDREN,
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
