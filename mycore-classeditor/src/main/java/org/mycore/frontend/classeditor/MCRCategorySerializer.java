package org.mycore.frontend.classeditor;

import java.lang.reflect.Type;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


public class MCRCategorySerializer implements JsonSerializer<MCRCategory> {

    public static final String DESCRIPTION = "descriptions";

    public static final String TEXT = "text";

    public static final String LABEL = "label";
    
    public static final String LANG = "lang";
    
    public static final String TAGNAME = "rubric";

    public static final String PARENTID = "parentID";

    public static final String CATEGID = "categID";

    public static final String ROOTID = "rootID";

    public static final String ID = "ID";

    @Override
    public JsonElement serialize(MCRCategory category, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject rubricJsonObject = new JsonObject();
        MCRCategory parentCateg = category.getParent();
        if (parentCateg != null) {
            rubricJsonObject.add(PARENTID, idToJsonObj(parentCateg.getId()));
        }

        MCRCategoryID id = category.getId();
        if (id != null) {
            rubricJsonObject.add(ID, idToJsonObj(id));
        }

        JsonArray labelJsonArray = labelsToJsonArray(category);
        rubricJsonObject.add(LABEL, labelJsonArray);
        return rubricJsonObject;
    }

    private JsonObject idToJsonObj(MCRCategoryID id) {
        JsonObject idJsonObj = new JsonObject();
        idJsonObj.addProperty(ROOTID, id.getRootID());
        String categID = id.getID();
        idJsonObj.addProperty(CATEGID, categID);

        return idJsonObj;
    }

    private JsonArray labelsToJsonArray(MCRCategory category) {
        JsonArray labelJsonArray = new JsonArray();
        for (MCRLabel label : category.getLabels()) {
            JsonObject labelJsonObj = labelToJsonObj(label);
            labelJsonArray.add(labelJsonObj);
        }
        return labelJsonArray;
    }

    private JsonObject labelToJsonObj(MCRLabel label) {
        JsonObject labelJsonObj = new JsonObject();
        labelJsonObj.addProperty(LANG, label.getLang());
        labelJsonObj.addProperty(TEXT, label.getText());
        labelJsonObj.addProperty(DESCRIPTION, label.getDescription());

        return labelJsonObj;
    }
}
