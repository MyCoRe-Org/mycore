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

package org.mycore.iiif.image.resources;

import static org.mycore.iiif.image.MCRIIIFImageUtil.buildCanonicalURL;
import static org.mycore.iiif.image.MCRIIIFImageUtil.buildProfileURL;
import static org.mycore.iiif.image.MCRIIIFImageUtil.completeProfile;
import static org.mycore.iiif.image.MCRIIIFImageUtil.encodeImageIdentifier;
import static org.mycore.iiif.image.MCRIIIFImageUtil.getIIIFURL;
import static org.mycore.iiif.image.MCRIIIFImageUtil.getImpl;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.iiif.common.MCRIIIFMediaTypeHelper;
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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@Path("/image/v2{noop: /?}{impl: ([a-zA-Z0-9]+)?}")
public class MCRIIIFImageResource {
    public static final String IIIF_IMAGE_API_2_LEVEL2 = "http://iiif.io/api/image/2/level2.json";

    public static final String IMPL_PARAM = "impl";

    public static final String IDENTIFIER_PARAM = "identifier";

    private static final Logger LOGGER = LogManager.getLogger();

    @Context
    Request request;

    Optional<Response> getCachedResponse(long lastModified) {
        return Optional.ofNullable(request)
            .map(r -> r.evaluatePreconditions(new Date(lastModified)))
            .map(Response.ResponseBuilder::build);
    }

    @GET
    @Produces(MCRIIIFMediaTypeHelper.APPLICATION_LD_JSON)
    @Path("{" + IDENTIFIER_PARAM + "}/info.json")
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    public Response getInfo(@PathParam(IMPL_PARAM) String implString, @PathParam(IDENTIFIER_PARAM) String identifier) {
        try {
            MCRIIIFImageImpl impl = getImpl(implString);
            MCRIIIFImageInformation information = impl.getInformation(identifier);

            Optional<Response> cachedResponse = getCachedResponse(information.lastModified);
            if (cachedResponse.isPresent()) {
                return cachedResponse.get();
            }

            MCRIIIFImageProfile profile = getProfile(impl);

            information.profile.add(IIIF_IMAGE_API_2_LEVEL2);
            information.profile.add(profile);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return Response.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Link", buildCanonicalURL(impl, identifier))
                .header("Profile", buildProfileURL())
                .lastModified(new Date(information.lastModified))
                .entity(gson.toJson(information))
                .build();
        } catch (MCRIIIFImageNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (MCRAccessException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        } catch (MCRIIIFImageProvidingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("{" + IDENTIFIER_PARAM + "}")
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    public Response getInfoRedirect(@PathParam(IMPL_PARAM) String impl,
        @PathParam(IDENTIFIER_PARAM) String identifier) {
        try {
            String uriString = getIIIFURL(getImpl(impl)) + encodeImageIdentifier(identifier) + "/info.json";
            return Response.temporaryRedirect(new URI(uriString)).build();
        } catch (URISyntaxException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("{" + IDENTIFIER_PARAM + "}/{region}/{size}/{rotation}/{quality}.{format}")
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    public Response getImage(@PathParam(IMPL_PARAM) String implStr, @PathParam(IDENTIFIER_PARAM) String identifier,
        @PathParam("region") String region,
        @PathParam("size") String size,
        @PathParam("rotation") String rotation,
        @PathParam("quality") String quality,
        @PathParam("format") String format) {
        try {
            MCRIIIFImageImpl impl = getImpl(implStr);
            MCRIIIFImageInformation information = impl.getInformation(identifier);

            Optional<Response> cachedResponse = getCachedResponse(information.lastModified);
            if (cachedResponse.isPresent()) {
                return cachedResponse.get();
            }

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
                .lastModified(new Date(information.lastModified))
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
    @Path("profile.json")
    @Produces(MCRIIIFMediaTypeHelper.APPLICATION_LD_JSON)
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 7, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 7, unit = TimeUnit.DAYS))
    public Response getDereferencedProfile(@PathParam(IMPL_PARAM) String implStr) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        MCRIIIFImageProfile profile = getProfile(getImpl(implStr));
        profile.setContext(MCRIIIFBase.API_IMAGE_2);
        return Response.ok().entity(gson.toJson(profile)).build();
    }

    public MCRIIIFImageProfile getProfile(MCRIIIFImageImpl impl) {
        MCRIIIFImageProfile profile = impl.getProfile();
        completeProfile(impl, profile);
        return profile;
    }

}
