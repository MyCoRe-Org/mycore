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
