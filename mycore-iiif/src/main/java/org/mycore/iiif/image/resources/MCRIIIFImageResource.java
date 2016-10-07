package org.mycore.iiif.image.resources;

import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.iiif.image.impl.MCRIIIFImageImpl;
import org.mycore.iiif.image.impl.MCRIIIFImageNotFoundException;
import org.mycore.iiif.image.impl.MCRIIIFImageProvidingException;
import org.mycore.iiif.image.impl.MCRIIIFUnsupportedFormatException;
import org.mycore.iiif.image.model.MCRIIIFImageInformation;
import org.mycore.iiif.image.model.MCRIIIFImageProfile;
import org.mycore.iiif.image.model.MCRIIIFImageQuality;
import org.mycore.iiif.image.model.MCRIIIFImageSourceRegion;
import org.mycore.iiif.image.model.MCRIIIFImageTargetRotation;
import org.mycore.iiif.image.model.MCRIIIFImageTargetSize;
import org.mycore.iiif.image.parser.MCRIIIFRegionParser;
import org.mycore.iiif.image.parser.MCRIIIFRotationParser;
import org.mycore.iiif.image.parser.MCRIIIFScaleParser;
import org.mycore.iiif.model.MCRIIIFBase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static org.mycore.iiif.image.MCRIIIFImageUtil.*;

@Path("/iiif/image")
public class MCRIIIFImageResource {
    public static final String IIIF_IMAGE_API_2_LEVEL2 = "http://iiif.io/api/image/2/level2.json";

    private static final Logger LOGGER = LogManager.getLogger();

    @GET
    @Produces("application/ld+json")
    @Path("{impl}/{identifier}/info.json")
    public Response getInfo(@PathParam("impl") String implString, @PathParam("identifier") String identifier) {
        try {
            MCRIIIFImageImpl impl = getImpl(implString);
            MCRIIIFImageInformation information = impl.getInformation(identifier);
            MCRIIIFImageProfile profile = getProfile(impl);

            information.profile.add(IIIF_IMAGE_API_2_LEVEL2);
            information.profile.add(profile);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return Response.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Link", buildCanonicalURL(impl, identifier))
                .header("Profile", buildProfileURL())
                .entity(gson.toJson(information))
                .build();
        } catch (MCRIIIFImageNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (MCRIIIFImageProvidingException | UnsupportedEncodingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (MCRAccessException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("{impl}/{identifier}")
    public Response getInfoRedirect(@PathParam("impl") String impl, @PathParam("identifier") String identifier) {
        try {
            String uriString = getIIIFURL(getImpl(impl)) + URLEncoder.encode(identifier, "UTF-8") + "/info.json";
            return Response.temporaryRedirect(new URI(uriString)).build();
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("{impl}/{identifier}/{region}/{size}/{rotation}/{<quality}.{format}")
    public Response getImage(@PathParam("impl") String implStr,
        @PathParam("identifier") String identifier,
        @PathParam("region") String region,
        @PathParam("size") String size,
        @PathParam("rotation") String rotation,
        @PathParam("quality") String quality,
        @PathParam("format") String format) {
        try {
            MCRIIIFImageImpl impl = getImpl(implStr);
            MCRIIIFImageInformation information = impl.getInformation(identifier);

            MCRIIIFRegionParser rp = new MCRIIIFRegionParser(region, information.width, information.height);
            MCRIIIFImageSourceRegion sourceRegion = rp.parseImageRegion();

            MCRIIIFScaleParser sp = new MCRIIIFScaleParser(size, sourceRegion.getX2() - sourceRegion.getX1(),
                sourceRegion.getY2() - sourceRegion.getY1());
            MCRIIIFImageTargetSize targetSize = sp.parseTargetScale();

            MCRIIIFRotationParser rotationParser = new MCRIIIFRotationParser(rotation);
            MCRIIIFImageTargetRotation parsedRotation = rotationParser.parse();

            MCRIIIFImageQuality imageQuality = MCRIIIFImageQuality.fromString(quality);

            BufferedImage provide = impl
                .provide(identifier, sourceRegion, targetSize, parsedRotation, imageQuality, format);

            Response.Status status = rp.isCompleteValid() ? Response.Status.OK : Response.Status.BAD_REQUEST;

            Response.ResponseBuilder responseBuilder = Response.status(status);
            return responseBuilder
                .header("Link", buildCanonicalURL(impl, identifier))
                .header("Profile", buildProfileURL())
                .type("image/" + format)
                .entity((StreamingOutput) outputStream -> ImageIO.write(provide, format, outputStream)).build();
        } catch (MCRIIIFImageNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (IllegalArgumentException | MCRIIIFUnsupportedFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (MCRAccessException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        } catch (Exception e) {
            LOGGER.error(() -> "Error while getting Image " + identifier + " from " + implStr + " with region: " +
                region + ", size: " + size + ", rotation: " + rotation + ", quality: " + quality + ", format: " +
                format, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("{impl}/profile.json")
    @Produces("application/ld+json")
    public Response getDereferencedProfile(MCRIIIFImageImpl impl) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        MCRIIIFImageProfile profile = getProfile(impl);
        profile.setContext(MCRIIIFBase.API_IMAGE_2);
        return Response.ok().entity(gson.toJson(profile)).build();
    }

    public MCRIIIFImageProfile getProfile(MCRIIIFImageImpl impl) {
        MCRIIIFImageProfile profile = impl.getProfile();
        completeProfile(impl, profile);
        return profile;
    }

}
