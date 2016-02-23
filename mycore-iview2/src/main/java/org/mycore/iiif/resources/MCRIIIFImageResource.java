package org.mycore.iiif.resources;

import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.mycore.access.MCRAccessException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.iiif.MCRIIIFImageImpl;
import org.mycore.iiif.model.MCRIIIFImageInformation;
import org.mycore.iiif.model.MCRIIIFImageQuality;
import org.mycore.iiif.model.MCRIIIFImageSourceRegion;
import org.mycore.iiif.model.MCRIIIFImageTargetRotation;
import org.mycore.iiif.model.MCRIIIFImageTargetSize;
import org.mycore.iiif.model.MCRIIIFProfile;
import org.mycore.iiif.parser.MCRIIIFRegionParser;
import org.mycore.iiif.parser.MCRIIIFRotationParser;
import org.mycore.iiif.parser.MCRIIIFScaleParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Path("/iiif/image")
public class MCRIIIFImageResource {
    public static final String IIIF_IMAGE_API_2_LEVEL2 = "http://iiif.io/api/image/2/level2.json";
//http://141.35.23.195:8291/mir/rsc/iiif/image/mir_derivate_00000001%2Fperthes_1855_0003.jpg/info.json

    @GET
    @Produces("application/ld+json")
    @Path("{identifier}/info.json")
    public Response getInfo(@PathParam("identifier") String identifier) {
        try {
            MCRIIIFImageInformation information = getImpl().getInformation(identifier);
            MCRIIIFProfile profile = getProfile();

            information.profile.add(IIIF_IMAGE_API_2_LEVEL2);
            information.profile.add(profile);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return Response.ok()
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Link", buildCanonicalURL(identifier))
                    .header("Profile",  buildProfileURL())
                    .entity(gson.toJson(information))
                    .build();
        } catch (MCRIIIFImageImpl.ImageNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (MCRIIIFImageImpl.ProvidingException | UnsupportedEncodingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (MCRAccessException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }
    }


    @GET
    @Path("{identifier}")
    public Response getInfoRedirect(@PathParam("identifier") String identifier) {
        try {
            String uriString = getIIIFURL() + URLEncoder.encode(identifier, "UTF-8") + "/info.json";
            return Response.temporaryRedirect(new URI(uriString)).build();
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }


    @GET
    @Path("{identifier}/{region}/{size}/{rotation}/{quality}.{format}")
    public Response getImage(@PathParam("identifier") String identifier,
                             @PathParam("region") String region,
                             @PathParam("size") String size,
                             @PathParam("rotation") String rotation,
                             @PathParam("quality") String quality,
                             @PathParam("format") String format) {
        try {
            MCRIIIFImageInformation information = getImpl().getInformation(identifier);


            MCRIIIFRegionParser rp = new MCRIIIFRegionParser(region, information.width, information.height);
            MCRIIIFImageSourceRegion sourceRegion = rp.parseImageRegion();

            MCRIIIFScaleParser sp = new MCRIIIFScaleParser(size, sourceRegion.getX2() - sourceRegion.getX1(), sourceRegion.getY2() - sourceRegion.getY1());
            MCRIIIFImageTargetSize targetSize = sp.parseTargetScale();

            MCRIIIFRotationParser rotationParser = new MCRIIIFRotationParser(rotation);
            MCRIIIFImageTargetRotation parsedRotation = rotationParser.parse();

            MCRIIIFImageQuality imageQuality = MCRIIIFImageQuality.fromString(quality);

            BufferedImage provide = getImpl().provide(identifier, sourceRegion, targetSize, parsedRotation, imageQuality, format);

            Response.Status status = rp.isCompleteValid() ? Response.Status.OK : Response.Status.BAD_REQUEST;

            Response.ResponseBuilder responseBuilder = Response.status(status);
            return responseBuilder
                    .header("Link", buildCanonicalURL(identifier))
                    .header("Profile",  buildProfileURL())
                    .type("image/" + format)
                    .entity((StreamingOutput) outputStream -> ImageIO.write(provide, format, outputStream)).build();
        } catch (MCRIIIFImageImpl.ImageNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (IllegalArgumentException | MCRIIIFImageImpl.UnsupportedFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (MCRIIIFImageImpl.ProvidingException | UnsupportedEncodingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (MCRAccessException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("profile.json")
    @Produces("application/ld+json")
    public Response getDereferencedProfile() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        MCRIIIFProfile profile = getProfile();
        profile.context = "http://iiif.io/api/image/2/context.json";
        return Response.ok().entity(gson.toJson(profile)).build();
    }

    private MCRIIIFProfile getProfile() {
        MCRIIIFProfile profile = getImpl().getProfile();
        completeProfile(profile);
        return profile;
    }

    private void completeProfile(MCRIIIFProfile profile) {
        profile.id = getProfileLink();
    }


    private String buildCanonicalURL(@PathParam("identifier") String identifier) throws UnsupportedEncodingException {
        return "<" + getIIIFURL() + URLEncoder.encode(identifier, "UTF-8") + "/full/full/0/color.jpg>;rel=\"canonical\"";
    }

    private String buildProfileURL() throws UnsupportedEncodingException {
        return String.format(Locale.ROOT, "<" + IIIF_IMAGE_API_2_LEVEL2 + ">;rel=\"profile\"");
    }

    private String getIIIFURL() {
        return MCRFrontendUtil.getBaseURL() + "rsc/iiif/image/";
    }

    private String getProfileLink() {
        return getIIIFURL() + "profile.json";
    }

    private MCRIIIFImageImpl getImpl() {
        return MCRConfiguration.instance().getSingleInstanceOf("MCR.IIIFImage.impl");
    }
}
