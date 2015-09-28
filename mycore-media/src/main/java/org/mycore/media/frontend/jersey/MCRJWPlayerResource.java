/**
 * 
 */
package org.mycore.media.frontend.jersey;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRAbstractFileStore;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.filter.MCRSecureTokenV2FilterConfig;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.frontend.support.MCRSecureTokenV2;

import com.google.gson.Gson;
import com.sun.jersey.api.json.JSONWithPadding;

/**
 * @author Thomas Scheffler (yagee)
 */
@Path("jwplayer")
public class MCRJWPlayerResource {
    @Context
    private HttpServletRequest request;

    @GET
    @Path("{derivateId}/{path: .+}/sources.js")
    @Produces({ "application/javascript" })
    public JSONWithPadding getSources(@PathParam("derivateId") String derivateId, @PathParam("path") String path,
        @QueryParam("callback") String callback)
            throws URISyntaxException, IOException {
        return new JSONWithPadding(getSources(derivateId, path), callback);
    }

    @GET
    @Path("{derivateId}/{path: .+}/sources.json")
    @Produces({ "application/javascript" })
    public String getSources(@PathParam("derivateId") String derivateId, @PathParam("path") String path)
        throws URISyntaxException, IOException {
        MCRObjectID derivate = MCRJerseyUtil.getID(derivateId);
        MCRJerseyUtil.checkDerivateReadPermission(derivate);
        try {
            String contentPath = getContentPath(derivateId, path);
            URLFormatter formatter = new URLFormatter(derivate, path, contentPath, request);
            return formatter.toJSON();
        } catch (NoSuchFileException e) {
            LogManager.getLogger().warn("Could not find video file.", e);
            throw new WebApplicationException(Status.NOT_FOUND);
        }
    }

    private static String getContentPath(String derivateId, String path)
        throws IOException {
        MCRPath mcrPath = MCRPath.getPath(derivateId, path);
        MCRAbstractFileStore fileStore = (MCRAbstractFileStore) Files.getFileStore(mcrPath);
        java.nio.file.Path absolutePath = fileStore.getPhysicalPath(mcrPath);
        java.nio.file.Path relativePath = fileStore.getBaseDirectory().relativize(absolutePath);
        LogManager.getLogger().info("{} -> {} -> {}", mcrPath, absolutePath, relativePath);
        //TODO: Wowza on Windows requires '\' as path separator, have to check that 
        return URLEncoder.encode(relativePath.toString().replace('/', '\\'), "UTF-8");
    }

    private static final class URLFormatter {
        private static String wowzaBaseURL = MCRConfiguration.instance().getString("MCR.Media.JWPlayer.Wowza.BaseURL");

        private static String wowzaRTMPBaseURL = MCRConfiguration.instance()
            .getString("MCR.Media.JWPlayer.Wowza.RTMPBaseURL");

        private static String wowzaContentPathPrefix = MCRConfiguration.instance()
            .getString("MCR.Media.JWPlayer.Wowza.ContentPathPrefix");

        private static String wowzaSharedSecret = MCRConfiguration.instance()
            .getString("MCR.Media.JWPlayer.Wowza.SharedSecred");

        private static String wowzaHashParameter = MCRConfiguration.instance()
            .getString("MCR.Media.JWPlayer.Wowza.HashParameter", "wowzatokenhash");

        private HttpServletRequest request;

        private MCRSecureTokenV2 wowzaToken = null;

        private String json;

        public URLFormatter(MCRObjectID derivate, String path, String contentPath, HttpServletRequest request)
            throws URISyntaxException {
            if (!wowzaBaseURL.isEmpty()) {
                wowzaToken = new MCRSecureTokenV2(wowzaContentPathPrefix + contentPath,
                    MCRFrontendUtil.getRemoteAddr(request),
                    wowzaSharedSecret, Arrays.stream(request.getQueryString().split("&"))
                        .filter(p -> !p.startsWith("callback="))
                        .collect(Collectors.joining("&")));
            }
            this.request = request;
            ArrayList<Source> sources = new ArrayList<>(4);
            add(sources, getDashStream(), "dash");
            if (mayContainHLSStream()) {
                add(sources, getHLSStream(), "hls");
            }
            add(sources, getRTMPStream(), "rtmp");
            add(sources, getPseudoStream(derivate, path), "mp4");
            Gson gson = new Gson();
            json = gson.toJson(sources.toArray(new Source[sources.size()]));
        }

        private static void add(ArrayList<Source> sources, String url, String type) {
            if (url != null) {
                sources.add(new Source(url, type));
            }
        }

        private String getDashStream() throws URISyntaxException {
            return wowzaToken == null ? null
                : wowzaToken.toURI(wowzaBaseURL, "/Manifest", wowzaHashParameter).toString();
        }

        private String getHLSStream() throws URISyntaxException {
            return wowzaToken == null ? null
                : wowzaToken.toURI(wowzaBaseURL, "/playlist.m3u8", wowzaHashParameter).toString();
        }

        private String getRTMPStream() throws URISyntaxException {
            return (wowzaToken != null && !wowzaRTMPBaseURL.isEmpty())
                ? wowzaToken.toURI(wowzaRTMPBaseURL, wowzaHashParameter).toString() : null;
        }

        private String getPseudoStream(MCRObjectID derivate, String path) {
            return MCRSecureTokenV2FilterConfig.getFileNodeServletSecured(derivate, path);
        }

        private boolean mayContainHLSStream() {
            //Gecko on Android will not work if we submit HLS stream
            String userAgent = request.getHeader("User-Agent");
            return !(userAgent.contains("Android") && userAgent.contains("Gecko"));
        }

        public String toJSON() {
            return json;
        }

    }

    private static class Source {
        @SuppressWarnings("unused")
        private String file;

        @SuppressWarnings("unused")
        private String type;

        public Source(String file, String type) {
            LogManager.getLogger().info("file : " + file);
            this.file = file;
            this.type = type;
        }
    }

}
