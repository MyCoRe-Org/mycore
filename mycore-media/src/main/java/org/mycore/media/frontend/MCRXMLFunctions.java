package org.mycore.media.frontend;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.mycore.media.video.MCRMediaSource;
import org.mycore.media.video.MCRMediaSourceProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MCRXMLFunctions {

    private static final String[] EMPTY_ARRAY = new String[0];

    public static NodeList getSources(String derivateId, String path)
        throws IOException, ParserConfigurationException, URISyntaxException {
        return getSources(derivateId, path, null);
    }

    public static NodeList getSources(String derivateId, String path, String userAgent)
        throws IOException, ParserConfigurationException, URISyntaxException {
        MCRMediaSourceProvider provider = new MCRMediaSourceProvider(derivateId, path, Optional.ofNullable(userAgent),
            () -> EMPTY_ARRAY);
        List<MCRMediaSource> sources = provider.getSources();
        Document document = DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .newDocument();
        return new NodeList() {

            @Override
            public Node item(int index) {
                Element source = document.createElement("source");
                source.setAttribute("src", sources.get(index).getUri());
                source.setAttribute("type", sources.get(index).getType().getMimeType());
                return source;
            }

            @Override
            public int getLength() {
                return sources.size();
            }
        };
    }

}
