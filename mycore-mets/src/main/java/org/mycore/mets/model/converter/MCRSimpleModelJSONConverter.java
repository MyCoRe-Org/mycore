package org.mycore.mets.model.converter;

import org.mycore.mets.model.simple.MCRMetsAltoLink;
import org.mycore.mets.model.simple.MCRMetsLink;
import org.mycore.mets.model.simple.MCRMetsSimpleModel;

import com.google.gson.GsonBuilder;

/**
 * This class converts MCRMetsSimpleModel to JSON
 * @author Sebastian Hofmann(mcrshofm)
 */
public class MCRSimpleModelJSONConverter {

    public static String toJSON(MCRMetsSimpleModel model) {
        GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(MCRMetsLink.class, new MCRMetsLinkTypeAdapter());
        gsonBuilder.registerTypeAdapter(MCRMetsAltoLink.class, new MCRAltoLinkTypeAdapter());
        gsonBuilder.setPrettyPrinting();

        return gsonBuilder.create().toJson(model);
    }

}
