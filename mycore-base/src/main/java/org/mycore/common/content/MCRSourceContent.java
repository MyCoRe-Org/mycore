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

package org.mycore.common.content;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Parent;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRURIResolver;

import jakarta.xml.bind.util.JAXBSource;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRSourceContent extends MCRWrappedContent {
    private static final MCRURIResolver URI_RESOLVER = MCRURIResolver.instance();

    private Source source;

    public MCRSourceContent(Source source) {
        Objects.requireNonNull(source, "Source cannot be null");
        this.source = source;
        MCRContent baseContent = switch (source) {
            case JDOMSource src -> getBaseContent(src);
            case JAXBSource jaxbSource -> getBaseContent(jaxbSource);
            case SAXSource src -> new MCRSAXContent(src.getXMLReader(), src.getInputSource());
            case DOMSource domSource -> new MCRDOMContent(domSource.getNode().getOwnerDocument());
            case StreamSource streamSource -> getBaseContent(streamSource);
            default -> null;
        };
        if (baseContent == null) {
            throw new MCRException("Could not get MCRContent from " + source.getClass().getCanonicalName()
                + ", systemId:" + source.getSystemId());
        }
        baseContent.setSystemId(getSystemId());
        this.setBaseContent(baseContent);
    }

    private static MCRContent getBaseContent(StreamSource streamSource) {
        InputStream inputStream = streamSource.getInputStream();
        if (inputStream != null) {
            return new MCRStreamContent(inputStream);
        } else {
            try {
                URI uri = new URI(streamSource.getSystemId());
                return new MCRURLContent(uri.toURL());
            } catch (URISyntaxException | MalformedURLException e) {
                throw new MCRException("Could not create instance of MCRURLContent for SYSTEMID: "
                    + streamSource.getSystemId(), e);
            }
        }
    }
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    //false positive, is used
    private static MCRByteContent getBaseContent(JAXBSource source) {
        TransformerFactory transformerFactory = TransformerFactory.newDefaultInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            transformer.transform(source, new StreamResult(bout));
            return new MCRByteContent(bout.toByteArray());
        } catch (TransformerException e) {
            throw new MCRException("Error while resolving JAXBSource", e);
        }
    }
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    //false positive, is used
    private static MCRJDOMContent getBaseContent(JDOMSource src) {
        Document xml = src.getDocument();
        if (xml != null) {
            return new MCRJDOMContent(xml);
        }
        return src.getNodes().stream()
            .filter(Parent.class::isInstance) //only Element or Document matter
            .map(Parent.class::cast)
            .findFirst()
            .map(p -> switch (p){
                case Element element when element.isRootElement() -> new MCRJDOMContent(element.getDocument());
                case Element element when element.getParent() == null -> new MCRJDOMContent(element);
                case Element element -> new MCRJDOMContent(element.clone());
                case Document doc -> new MCRJDOMContent(doc);
                default -> null;
            })
            .orElse(null);
    }

    /**
     * Build instance of MCRSourceContent by resolving via {@link MCRURIResolver}
     * 
     * @throws TransformerException
     *             thrown by {@link MCRURIResolver#resolve(String, String)}
     */
    public static MCRSourceContent getInstance(String uri) throws TransformerException {
        Source source = URI_RESOLVER.resolve(uri, null);
        if (source == null) {
            return null;
        }
        return new MCRSourceContent(source);
    }

    @Override
    public String getSystemId() {
        return source.getSystemId();
    }

    @Override
    public Source getSource() {
        return source;
    }
}
