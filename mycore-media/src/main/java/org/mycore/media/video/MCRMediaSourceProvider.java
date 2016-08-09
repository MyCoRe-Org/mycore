package org.mycore.media.video;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRAbstractFileStore;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.filter.MCRSecureTokenV2FilterConfig;
import org.mycore.frontend.support.MCRSecureTokenV2;
import org.mycore.media.MCRMediaSourceType;

public class MCRMediaSourceProvider {
    private static Optional<String> wowzaBaseURL = MCRConfiguration2.getString("MCR.Media.Wowza.BaseURL");

    private static Optional<String> wowzaRTMPBaseURL = MCRConfiguration2
        .getString("MCR.Media.Wowza.RTMPBaseURL");

    private static String wowzaHashParameter = MCRConfiguration2.getString("MCR.Media.Wowza.HashParameter")
        .orElse("wowzatokenhash");

    static {
        MCRConfiguration2.addPropertyChangeEventLister(p -> p.startsWith("MCR.Media.Wowza"),
            MCRMediaSourceProvider::updateWowzaSettings);
    }

    private Optional<MCRSecureTokenV2> wowzaToken;

    private ArrayList<MCRMediaSource> sources;

    public MCRMediaSourceProvider(String derivateId, String path, Optional<String> userAgent,
        Supplier<String[]> parameterSupplier) throws IOException, URISyntaxException {
        try {
            wowzaToken = wowzaBaseURL.map(
                (w) -> new MCRSecureTokenV2(
                    MCRConfiguration2.getStringOrThrow("MCR.Media.Wowza.ContentPathPrefix")
                        + getContentPath(derivateId, path),
                    MCRSessionMgr.getCurrentSession().getCurrentIP(),
                    MCRConfiguration2.getStringOrThrow("MCR.Media.Wowza.SharedSecred"),
                    parameterSupplier.get()));
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                }
                if (cause instanceof URISyntaxException) {
                    throw (URISyntaxException) cause;
                }
            }
            throw e;
        }
        ArrayList<MCRMediaSource> mediaSources = new ArrayList<>(4);
        getDashStream()
            .map(s -> new MCRMediaSource(s, MCRMediaSourceType.dash_stream))
            .ifPresent(mediaSources::add);
        userAgent.filter(MCRMediaSourceProvider::mayContainHLSStream).ifPresent(f -> {
            getHLSStream()
                .map(s -> new MCRMediaSource(s, MCRMediaSourceType.hls_stream))
                .ifPresent(mediaSources::add);
        });
        getRTMPStream()
            .map(s -> new MCRMediaSource(s, MCRMediaSourceType.rtmp_stream))
            .ifPresent(mediaSources::add);
        mediaSources.add(
            new MCRMediaSource(getPseudoStream(MCRObjectID.getInstance(derivateId), path), MCRMediaSourceType.mp4));
        this.sources = mediaSources;
    }

    public static void updateWowzaSettings(String propertyName, Optional<String> oldValue,
        Optional<String> newValue) {
        switch (propertyName) {
            case "MCR.Media.Wowza.BaseURL":
                wowzaBaseURL = newValue;
                break;
            case "MCR.Media.Wowza.RTMPBaseURL":
                wowzaRTMPBaseURL = newValue;
                break;
            case "MCR.Media.Wowza.HashParameter":
                wowzaHashParameter = newValue.orElse("wowzatokenhash");
                break;
            default:
                break;
        }
    }

    public List<MCRMediaSource> getSources() {
        return Collections.unmodifiableList(sources);
    }

    private Optional<String> toURL(MCRSecureTokenV2 token, Optional<String> baseURL, String suffix,
        String hashParameterName) {
        return baseURL.map(b -> {
            try {
                return token.toURI(b, suffix, hashParameterName).toString();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Optional<String> getDashStream() {
        return wowzaToken.flatMap(w -> toURL(w, wowzaBaseURL, "/manifest.mpd", wowzaHashParameter));
    }

    private Optional<String> getHLSStream() {
        return wowzaToken.flatMap(w -> toURL(w, wowzaBaseURL, "/playlist.m3u8", wowzaHashParameter));
    }

    private Optional<String> getRTMPStream() {
        return wowzaToken.flatMap(w -> toURL(w, wowzaRTMPBaseURL, "", wowzaHashParameter));
    }

    private String getPseudoStream(MCRObjectID derivate, String path) {
        return MCRSecureTokenV2FilterConfig.getFileNodeServletSecured(derivate, path);
    }

    private static String getContentPath(String derivateId, String path) {
        try {
            MCRPath mcrPath = MCRPath.getPath(derivateId, path);
            MCRAbstractFileStore fileStore = (MCRAbstractFileStore) Files.getFileStore(mcrPath);
            java.nio.file.Path absolutePath = fileStore.getPhysicalPath(mcrPath);
            java.nio.file.Path relativePath = fileStore.getBaseDirectory().relativize(absolutePath);
            LogManager.getLogger().info("{} -> {} -> {}", mcrPath, absolutePath, relativePath);
            return relativePath.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean mayContainHLSStream(String userAgent) {
        //Gecko on Android will not work if we submit HLS stream
        return !(userAgent.contains("Android") && userAgent.contains("Gecko"));
    }
}
