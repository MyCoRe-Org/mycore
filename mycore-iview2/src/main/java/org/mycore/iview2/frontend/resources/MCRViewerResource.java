package org.mycore.iview2.frontend.resources;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.iview2.frontend.MCRIviewACLProvider;
import org.mycore.iview2.frontend.MCRIviewDefaultACLProvider;
import org.mycore.iview2.frontend.configuration.MCRViewerConfiguration;
import org.mycore.iview2.frontend.configuration.MCRViewerConfigurationStrategy;
import org.mycore.iview2.frontend.configuration.MCRViewerDefaultConfigurationStrategy;
import org.mycore.services.i18n.MCRTranslation;
import org.xml.sax.SAXException;

import static org.mycore.common.xml.MCRLayoutService.getContentTransformer;

/**
 * Base resource for the mycore image viewer.
 * 
 * @author Matthias Eichner
 * @author Sebastian Hofmann
 */
@Path("/viewer")
public class MCRViewerResource {

    private static final MCRIviewACLProvider IVIEW_ACL_PROVDER = MCRConfiguration.instance()
        .<MCRIviewACLProvider> getInstanceOf("MCR.Module-iview2.MCRIviewACLProvider",
            MCRIviewDefaultACLProvider.class.getName());

    private static final String JSON_CONFIG_ELEMENT_NAME = "json";

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("{derivate}{path: (/[^?#]*)?}")
    public Response show(@Context HttpServletRequest request, @Context Request jaxReq,
        @Context ServletContext context, @Context ServletConfig config) throws Exception {
        MCRContent content = getContent(request);
        String contentETag = content.getETag();
        Response.ResponseBuilder responseBuilder = null;
        EntityTag eTag = contentETag == null ? null : new EntityTag(contentETag);
        if (eTag != null) {
            responseBuilder = jaxReq.evaluatePreconditions(eTag);
        }
        if (responseBuilder == null) {
            responseBuilder = Response.ok(content.asByteArray(), MediaType.valueOf(content.getMimeType()));
        }
        if (eTag != null) {
            responseBuilder.tag(eTag);
        }
        if (content.isUsingSession()) {
            CacheControl cc = new CacheControl();
            cc.setPrivate(true);
            cc.setMaxAge(0);
            cc.setMustRevalidate(true);
            responseBuilder.cacheControl(cc);
        }
        return responseBuilder.build();
    }

    /**
     * Builds the jdom configuration response document.
     * 
     * @param config the mycore configuration object
     * @return jdom configuration object
     */
    private static Document buildResponseDocument(MCRViewerConfiguration config)
        throws JDOMException, IOException, SAXException, JAXBException {
        String configJson = config.toJSON();
        Element startIviewClientElement = new Element("IViewConfig");
        Element configElement = new Element(JSON_CONFIG_ELEMENT_NAME);
        startIviewClientElement.addContent(configElement);
        startIviewClientElement.addContent(config.toXML().asXML().getRootElement().detach());
        configElement.addContent(configJson);
        Document startIviewClientDocument = new Document(startIviewClientElement);
        return startIviewClientDocument;
    }

    protected MCRContent getContent(final HttpServletRequest req)
        throws Exception {
        // get derivate id from request object
        String derivate = MCRViewerConfiguration.getDerivate(req);
        if (derivate == null) {
            MCRJerseyUtil.throwException(Status.BAD_REQUEST, "Could not locate derivate identifer in path.");
        }
        // get mycore object id
        final MCRObjectID derivateID = MCRObjectID.getInstance(derivate);
        if (!MCRMetadataManager.exists(derivateID)) {
            String errorMessage = MCRTranslation.translate("component.iview2.MCRIViewClientServlet.object.not.found",
                derivateID);
            MCRJerseyUtil.throwException(Status.NOT_FOUND, errorMessage);
        }
        // check permission
        if (IVIEW_ACL_PROVDER != null && !IVIEW_ACL_PROVDER.checkAccess(req.getSession(), derivateID)) {
            String errorMessage = MCRTranslation.translate("component.iview2.MCRIViewClientServlet.noRights",
                derivateID);
            MCRJerseyUtil.throwException(Status.UNAUTHORIZED, errorMessage);
        }
        // build configuration object
        MCRViewerConfigurationStrategy configurationStrategy = MCRConfiguration.instance()
            .getInstanceOf("MCR.Module-iview2.configuration.strategy", new MCRViewerDefaultConfigurationStrategy());
        MCRJDOMContent source = new MCRJDOMContent(buildResponseDocument(configurationStrategy.get(req)));
        MCRParameterCollector parameter = new MCRParameterCollector(req);
        MCRContentTransformer transformer = getContentTransformer(source.getDocType(), parameter);
        return transformer.transform(source);
    }

}
