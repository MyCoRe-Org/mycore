package org.mycore.viewer.configuration;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.services.i18n.MCRTranslation;

public class MCRViewerMetadataConfiguration extends MCRViewerConfiguration {

    private static final int EXPIRE_METADATA_CACHE_TIME = 10; // in seconds

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        String derivate = getDerivate(request);
        MCRObjectID derivateID = MCRObjectID.getInstance(derivate);
        final MCRObjectID objectID = MCRMetadataManager.getObjectId(derivateID, EXPIRE_METADATA_CACHE_TIME,
            TimeUnit.SECONDS);
        if (objectID == null) {
            String errorMessage = MCRTranslation.translate("component.viewer.MCRIViewClientServlet.object.not.found",
                objectID);
            // TODO: we should not throw an webapplication exc. here -> instead throw something líke ConfigException
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(errorMessage).build());
        }

        // properties
        setProperty("objId", objectID.toString());
        String urlFormat = "%sreceive/%s?XSL.Transformer=%s";
        MCRConfiguration mcrConfiguration = MCRConfiguration.instance();
        String transformer = mcrConfiguration.getString("MCR.Viewer.metadata.transformer", null);
        if (transformer != null) {
            setProperty(
                "metadataURL",
                String.format(Locale.ROOT, urlFormat, MCRFrontendUtil.getBaseURL(), objectID, transformer));
        }

        // script
        addLocalScript("iview-client-metadata.js", isDebugParameterSet(request));

        return this;
    }

}
