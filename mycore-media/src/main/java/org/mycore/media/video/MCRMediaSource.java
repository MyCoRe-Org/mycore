package org.mycore.media.video;

import org.apache.logging.log4j.LogManager;
import org.mycore.media.MCRMediaSourceType;

public class MCRMediaSource {
    private String uri;

    private MCRMediaSourceType type;

    public MCRMediaSource(String file, MCRMediaSourceType type) {
        LogManager.getLogger().info("uri : " + file);
        this.uri = file;
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public MCRMediaSourceType getType() {
        return type;
    }
}
