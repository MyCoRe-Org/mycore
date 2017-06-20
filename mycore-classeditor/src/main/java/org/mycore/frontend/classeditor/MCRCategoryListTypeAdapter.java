package org.mycore.frontend.classeditor;

import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.HASCHILDREN;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.HASLINK;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.ID;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.LABELS;
import static org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName.URISTR;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mycore.common.MCRJSONTypeAdapter;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.frontend.classeditor.json.MCRJSONCategory;
import org.mycore.frontend.classeditor.wrapper.MCRCategoryListWrapper;
import org.mycore.frontend.classeditor.wrapper.MCRLabelSetWrapper;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

public class MCRCategoryListTypeAdapter extends MCRJSONTypeAdapter<MCRCategoryListWrapper> {
    private JsonSerializationContext serializationContext;

    @Override
    public JsonElement serialize(MCRCategoryListWrapper categListWrapper, Type typeOfSrc,
        JsonSerializationContext context) {
        this.serializationContext = context;
        Map<MCRCategoryID, Boolean> linkMap = categListWrapper.getLinkMap();

        if (linkMap == null) {
            throw new RuntimeException("For serializing link map must not be null.");
        }

        return categListToJsonArray(categListWrapper.getList(), linkMap);
    }

    private JsonElement categListToJsonArray(List<MCRCategory> categList, Map<MCRCategoryID, Boolean> linkMap) {
        JsonArray categJsonArray = new JsonArray();
        for (MCRCategory categ : categList) {
            Boolean hasLink = linkMap.get(categ.getId());
            JsonElement element = createCategRefJSONObj(categ, hasLink);
            categJsonArray.add(element);
        }

        return categJsonArray;
    }

    private JsonElement createCategRefJSONObj(MCRCategory categ, Boolean hasLink) {
        JsonObject categRefJsonObject = new JsonObject();
        categRefJsonObject.add(ID, serializationContext.serialize(categ.getId()));
        Set<MCRLabel> labels = categ.getLabels();
        categRefJsonObject.add(LABELS, serializationContext.serialize(new MCRLabelSetWrapper(labels)));
        URI uri = categ.getURI();
        if (uri != null) {
            categRefJsonObject.addProperty(URISTR, uri.toString());
        }
        categRefJsonObject.addProperty(HASCHILDREN, categ.hasChildren());
        categRefJsonObject.addProperty(HASLINK, hasLink);
        return categRefJsonObject;
    }

    @Override
    public MCRCategoryListWrapper deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
        List<MCRCategory> categList = new ArrayList<MCRCategory>();

        for (JsonElement categRef : json.getAsJsonArray()) {
            JsonObject categRefJsonObject = categRef.getAsJsonObject();

            MCRCategory categ = context.deserialize(categRefJsonObject, MCRJSONCategory.class);
            categList.add(categ);
        }

        return new MCRCategoryListWrapper(categList);
    }
}
