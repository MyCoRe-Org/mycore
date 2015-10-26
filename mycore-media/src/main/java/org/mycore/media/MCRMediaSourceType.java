package org.mycore.media;

public enum MCRMediaSourceType {
    mp4, rtmp_stream, hls_stream, dash_stream;

    public String getSimpleType() {
        String str = toString();
        int pos = str.indexOf('_');
        return pos > 0 ? str.substring(0, pos) : str;
    }

    public String getMimeType() {
        switch (this) {
            case mp4:
                return "video/mp4";
            case hls_stream:
                return "application/x-mpegURL";
            case rtmp_stream:
                return "rtmp/mp4";
            case dash_stream:
                return "application/dash+xml";
            default:
                throw new RuntimeException(this + " has no MIME type defined.");
        }
    }
}
