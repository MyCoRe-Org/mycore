package org.mycore.iview2.frontend.configuration;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.iview2.services.MCRIView2Tools;
import org.mycore.services.i18n.MCRTranslation;

public class MCRIViewClientMetadataConfiguration extends MCRIViewClientConfiguration {

    private static final int EXPIRE_METADATA_CACHE_TIME = 10; // in seconds

    @Override
    public MCRIViewClientConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        String derivate = getDerivate(request);
        MCRObjectID derivateID = MCRObjectID.getInstance(derivate);
        final MCRObjectID objectID = MCRMetadataManager.getObjectId(derivateID, EXPIRE_METADATA_CACHE_TIME,
            TimeUnit.SECONDS);
        if (objectID == null) {
            String errorMessage = MCRTranslation.translate("component.iview2.MCRIViewClientServlet.object.not.found",
                objectID);
            // TODO: we should not throw an webapplication exc. here -> instead throw something líke ConfigException
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(errorMessage).build());
        }

        // properties
        setProperty("objId", objectID.toString());
        String urlFormat = "%sreceive/%s?XSL.Transformer=%s";
        setProperty("metadataURL", String.format(urlFormat, MCRFrontendUtil.getBaseURL(), objectID, MCRIView2Tools.getIView2Property("metadata.transformer")));

        // script
        addLocalScript("iview-client-metadata.js");

        return this;
    }

}
