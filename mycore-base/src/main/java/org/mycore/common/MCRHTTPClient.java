package org.mycore.common;

import java.io.IOException;
import java.net.URI;

import org.mycore.common.content.MCRContent;

public interface MCRHTTPClient {
    MCRContent get(URI hrefURI) throws IOException;

    void close();
}
