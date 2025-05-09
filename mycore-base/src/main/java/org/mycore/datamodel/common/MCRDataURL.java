/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
package org.mycore.datamodel.common;

import java.io.ByteArrayOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.mycore.common.MCRException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Represents the data URL scheme (<a href="https://tools.ietf.org/html/rfc2397">RFC2397</a>).
 *
 * @author René Adler (eagle)
 *
 */
public class MCRDataURL implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String SCHEME = "data:";

    private static final String MIME_TYPE_TEXT_XML = "text/xml";

    private static final String MIME_TYPE_TEXT_PLAIN = "text/plain";

    private static final String DEFAULT_MIMETYPE = MIME_TYPE_TEXT_PLAIN;

    private static final Pattern PATTERN_MIMETYPE = Pattern.compile("^([a-z0-9\\-\\+]+)\\/([a-z0-9\\-\\+]+)$");

    private static final String CHARSET_PARAM = "charset";

    private static final String TOKEN_SEPARATOR = ";";

    private static final String DATA_SEPARATOR = ",";

    private static final String PARAM_SEPARATOR = "=";

    private final String mimeType;

    private final Map<String, String> parameters;

    private final Charset charset;

    private final MCRDataURLEncoding encoding;

    private final byte[] data;

    /**
     * Constructs a new {@link MCRDataURL}.
     *
     * @param data the data
     * @param encoding the encoding of data url
     * @param mimeType the mimeType of data url
     * @param parameters a list of paramters of data url
     */
    public MCRDataURL(final byte[] data, final MCRDataURLEncoding encoding, final String mimeType,
        final Map<String, String> parameters) throws MalformedURLException {
        this.data = Arrays.copyOf(data, data.length);
        this.encoding = encoding != null ? encoding : MCRDataURLEncoding.URL;
        this.mimeType = mimeType != null && !mimeType.isEmpty() ? mimeType : DEFAULT_MIMETYPE;

        if (!PATTERN_MIMETYPE.matcher(this.mimeType).matches()) {
            throw new MalformedURLException("Unknown mime type.");
        }

        if (parameters != null) {
            this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters.entrySet()
                .stream()
                .filter(
                    e -> !CHARSET_PARAM.equals(e.getKey()))
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue))));
            this.charset = parameters.containsKey(CHARSET_PARAM) && parameters.get(CHARSET_PARAM) != null
                && !parameters.get(CHARSET_PARAM).isEmpty() ? Charset.forName(parameters.get(CHARSET_PARAM))
                    : StandardCharsets.US_ASCII;
        } else {
            this.parameters = Collections.emptyMap();
            this.charset = StandardCharsets.US_ASCII;
        }
    }

    /**
     * Constructs a new {@link MCRDataURL}.
     *
     * @param data the data
     * @param encoding the encoding of data url
     * @param mimeType the mimeType of data url
     * @param charset the charset of data url
     */
    public MCRDataURL(final byte[] data, final MCRDataURLEncoding encoding, final String mimeType,
        final Charset charset) throws MalformedURLException {
        this.data = Arrays.copyOf(data, data.length);
        this.encoding = encoding != null ? encoding : MCRDataURLEncoding.URL;
        this.mimeType = mimeType != null && !mimeType.isEmpty() ? mimeType : DEFAULT_MIMETYPE;

        if (!PATTERN_MIMETYPE.matcher(this.mimeType).matches()) {
            throw new MalformedURLException("Unknown mime type.");
        }
        this.parameters = Collections.emptyMap();
        this.charset = charset != null ? charset : StandardCharsets.US_ASCII;
    }

    /**
     * Constructs a new {@link MCRDataURL}.
     *
     * @param data the data
     * @param encoding the encoding of data url
     * @param mimeType the mimeType of data url
     * @param charset the charset of data url
     */
    public MCRDataURL(final byte[] data, final MCRDataURLEncoding encoding, final String mimeType, final String charset)
        throws MalformedURLException {
        this(data, encoding, mimeType, Charset.forName(charset));
    }

    /**
     * Constructs a new {@link MCRDataURL}.
     *
     * @param data the data
     * @param encoding the encoding of data url
     * @param mimeType the mimeType of data url
     */
    public MCRDataURL(final byte[] data, final MCRDataURLEncoding encoding, final String mimeType)
        throws MalformedURLException {
        this(data, encoding, mimeType, StandardCharsets.US_ASCII);
    }

    /**
     * Constructs a new {@link MCRDataURL}.
     *
     * @param data the data of data url
     * @param encoding the encoding of data url
     */
    public MCRDataURL(final byte[] data, final MCRDataURLEncoding encoding) throws MalformedURLException {
        this(data, encoding, DEFAULT_MIMETYPE, StandardCharsets.US_ASCII);
    }

    /**
     * Constructs a new {@link MCRDataURL}.
     *
     * @param data the data of data url
     */
    public MCRDataURL(final byte[] data) throws MalformedURLException {
        this(data, MCRDataURLEncoding.URL, DEFAULT_MIMETYPE, StandardCharsets.US_ASCII);
    }

    /**
     * Build a "data" URL for given {@link Document}, encoding, mime-type and charset.
     * Should encoding be <code>null</code>, it is detect from mime-type.
     *
     * @param document the document
     * @param encoding the {@link MCRDataURLEncoding}
     * @param mimeType the mime-type
     * @param charset the charset
     * @return a string with "data" URL
     */
    public static String build(final Document document, final String encoding, final String mimeType,
        final String charset) throws TransformerException, MalformedURLException {
        return build(document.getChildNodes(), encoding, mimeType, charset);
    }

    /**
     * Build a "data" URL for given {@link NodeList}, encoding, mime-type and charset.
     * Should encoding be <code>null</code>, it is detect from mime-type.
     *
     * @param nodeList the node list
     * @param encoding the {@link MCRDataURLEncoding}
     * @param mimeType the mime-type
     * @param charset the charset
     * @return a string with "data" URL
     */
    public static String build(final NodeList nodeList, final String encoding, final String mimeType,
        final String charset) throws TransformerException, MalformedURLException {
        Node node = Optional.ofNullable(nodeList.item(0)).filter(n -> n.getNodeName().equals("#document"))
            .orElseGet(() -> Optional.of(nodeList).filter(nl -> nl.getLength() == 1).map(nl -> nl.item(0))
                .orElseThrow(() -> new IllegalArgumentException("Nodelist must have an single root element.")));

        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final Transformer transformer = transformerFactory.newTransformer();

        MCRDataURLEncoding enc = encoding != null ? MCRDataURLEncoding.fromValue(encoding) : null;
        String method = "xml";

        final Matcher mtm = PATTERN_MIMETYPE.matcher(mimeType);
        if (mtm.matches()) {
            if (enc == null) {
                if ("text".equals(mtm.group(1))) {
                    enc = MCRDataURLEncoding.URL;
                } else {
                    enc = MCRDataURLEncoding.BASE64;
                }
            }

            if ("plain".equals(mtm.group(2))) {
                method = "text";
            } else if ("html".equals(mtm.group(2))) {
                method = "html";
            } else if ("xml|xhtml+xml".contains(mtm.group(2))) {
                method = "xml";
            } else {
                method = null;
            }
        }

        if (method != null) {
            transformer.setOutputProperty(OutputKeys.METHOD, method);
        }

        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, mimeType);
        transformer.setOutputProperty(OutputKeys.ENCODING, charset);

        DOMSource source = new DOMSource(node);
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(bao);
        transformer.transform(source, result);

        final MCRDataURL dataURL = new MCRDataURL(bao.toByteArray(), enc, mimeType, charset);

        return dataURL.toString();
    }

    /**
     * Build a "data" URL for given {@link String}, encoding, mime-type and charset.
     * Should encoding be <code>null</code>, it is detect from mime-type.
     *
     * @param str the value
     * @param encoding the {@link MCRDataURLEncoding}
     * @param mimeType the mime-type
     * @param charset the charset
     * @return a string with "data" URL
     */
    public static String build(final String str, final String encoding, final String mimeType, final String charset)
        throws MalformedURLException {
        MCRDataURLEncoding enc = encoding != null ? MCRDataURLEncoding.fromValue(encoding) : null;
        final Matcher mtm = PATTERN_MIMETYPE.matcher(mimeType);
        if (mtm.matches() && enc == null) {
            if ("text".equals(mtm.group(1))) {
                enc = MCRDataURLEncoding.URL;
            } else {
                enc = MCRDataURLEncoding.BASE64;
            }
        }

        final MCRDataURL dataURL = new MCRDataURL(str.getBytes(Charset.forName(charset)), enc, mimeType, charset);
        return dataURL.toString();
    }

    /**
     * Build a "data" URL for given {@link Document}, mime-type and <code>UTF-8</code> as charset.
     *
     * @param document the document
     * @param mimeType the mime-type
     * @return a string with "data" URL
     */
    public static String build(final Document document, final String mimeType)
        throws TransformerException, MalformedURLException {
        return build(document, null, mimeType, StandardCharsets.UTF_8.name());
    }

    /**
     * Build a "data" URL for given {@link NodeList}, mime-type and <code>UTF-8</code> as charset.
     *
     * @param nodeList the node list
     * @param mimeType the mime-type
     * @return a string with "data" URL
     */
    public static String build(final NodeList nodeList, final String mimeType)
        throws TransformerException, MalformedURLException {
        return build(nodeList, null, mimeType, StandardCharsets.UTF_8.name());
    }

    /**
     * Build a "data" URL for given {@link String}, mime-type and <code>UTF-8</code> as charset.
     *
     * @param str the string
     * @param mimeType the mime-type
     * @return a string with "data" URL
     */
    public static String build(final String str, final String mimeType)
        throws MalformedURLException {
        return build(str, null, mimeType, StandardCharsets.UTF_8.name());
    }

    /**
     * Build a "data" URL for given {@link Document} with mime-type based encoding,
     * <code>text/xml</code> as mime-type and <code>UTF-8</code> as charset.
     *
     * @param document the document
     * @return a string with "data" URL
     */
    public static String build(final Document document) throws TransformerException, MalformedURLException {
        return build(document, null, MIME_TYPE_TEXT_XML, StandardCharsets.UTF_8.name());
    }

    /**
     * Build a "data" URL for given {@link NodeList} with mime-type based encoding,
     * <code>text/xml</code> as mime-type and <code>UTF-8</code> as charset.
     *
     * @param nodeList the node list
     * @return a string with "data" URL
     */
    public static String build(final NodeList nodeList) throws TransformerException, MalformedURLException {
        return build(nodeList, null, MIME_TYPE_TEXT_XML, StandardCharsets.UTF_8.name());
    }

    /**
     * Build a "data" URL for given {@link String} with mime-type based encoding,
     * <code>text/xml</code> as mime-type and <code>UTF-8</code> as charset.
     *
     * @param str the node list
     * @return a string with "data" URL
     */
    public static String build(final String str) throws MalformedURLException {
        return build(str, null, MIME_TYPE_TEXT_PLAIN, StandardCharsets.UTF_8.name());
    }

    /**
     * Parse a {@link String} to {@link MCRDataURL}.
     *
     * @param dataURL the data url string
     * @return a {@link MCRDataURL} object
     */
    public static MCRDataURL parse(final String dataURL) throws MalformedURLException {
        final String url = dataURL.trim();
        if (url.startsWith(SCHEME)) {
            String[] parts = url.substring(SCHEME.length()).split(DATA_SEPARATOR, 2);
            if (parts.length == 2) {
                String[] tokens = parts[0].split(TOKEN_SEPARATOR);
                List<String> token = Arrays.stream(tokens)
                    .filter(s -> !s.contains(PARAM_SEPARATOR))
                    .toList();
                Map<String, String> params = parseTokenizedParameters(parts);
                final String mimeType = !token.isEmpty() ? token.getFirst() : null;

                if (mimeType != null && !mimeType.isEmpty() && !PATTERN_MIMETYPE.matcher(mimeType).matches()) {
                    throw new MalformedURLException("Unknown mime type.");
                }

                final MCRDataURLEncoding encoding;
                try {
                    encoding = !token.isEmpty() && token.size() > 1 ? MCRDataURLEncoding.fromValue(token.get(1))
                        : MCRDataURLEncoding.URL;
                } catch (IllegalArgumentException e) {
                    MalformedURLException malformedURLException = new MalformedURLException("Unknown encoding.");
                    malformedURLException.initCause(e);
                    throw malformedURLException;
                }

                Charset charset = params.containsKey(CHARSET_PARAM) ? Charset.forName(params.get(CHARSET_PARAM))
                    : StandardCharsets.US_ASCII;
                byte[] data;
                try {
                    data = encoding == MCRDataURLEncoding.BASE64 ? Base64.getDecoder().decode(parts[1])
                        : decode(parts[1], charset).getBytes(StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    MalformedURLException mue = new MalformedURLException("Error decoding the data.");
                    mue.initCause(e);
                    throw mue;
                }
                return new MCRDataURL(data, encoding, mimeType, params);
            } else {
                throw new MalformedURLException("Error parse data url: " + url);
            }
        } else {
            throw new MalformedURLException("Wrong protocol");
        }

    }

    private static Map<String, String> parseTokenizedParameters(String[] parts) {
        String[] tokens = parts[0].split(TOKEN_SEPARATOR);
        return Arrays.stream(tokens).filter(s -> s.contains(PARAM_SEPARATOR))
            .map(s -> s.split(PARAM_SEPARATOR, 2)).collect(Collectors.toMap(sl -> sl[0], sl -> {
                try {
                    return decode(sl[1], StandardCharsets.UTF_8);
                } catch (UnsupportedCharsetException e) {
                    throw new MCRException("Error decoding the parameter value \"" + sl[1] + "\".", e);
                }
            }));
    }

    private static String encode(final String str, final Charset charset) {
        return URLEncoder.encode(str, charset).replace("+", "%20");
    }

    private static String decode(final String str, final Charset charset) {
        return URLDecoder.decode(str.replace("%20", "+"), charset);
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @return the parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * @return the charset
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * @return the encoding
     */
    public MCRDataURLEncoding getEncoding() {
        return encoding;
    }

    /**
     * @return the data
     */
    public byte[] getData() {
        return data.clone();
    }

    /**
     * Returns a {@link String} of a {@link MCRDataURL} object .
     *
     * @return the data url as string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(SCHEME);

        if (!DEFAULT_MIMETYPE.equals(mimeType) || !charset.equals(StandardCharsets.US_ASCII)) {
            sb.append(mimeType);
        }

        if (!charset.equals(StandardCharsets.US_ASCII)) {
            sb.append(TOKEN_SEPARATOR + CHARSET_PARAM + PARAM_SEPARATOR).append(charset.name());
        }

        parameters.forEach((key, value) -> {
            sb.append(TOKEN_SEPARATOR)
                .append(key)
                .append(PARAM_SEPARATOR)
                .append(encode(value, StandardCharsets.UTF_8));
        });

        if (encoding == MCRDataURLEncoding.BASE64) {
            sb.append(TOKEN_SEPARATOR).append(encoding.value());
            sb.append(DATA_SEPARATOR).append(Base64.getEncoder().withoutPadding().encodeToString(data));
        } else {
            sb.append(DATA_SEPARATOR).append(encode(new String(data, charset), charset));
        }

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((charset == null) ? 0 : charset.hashCode());
        result = prime * result + ((data == null) ? 0 : Arrays.hashCode(data));
        result = prime * result + ((encoding == null) ? 0 : encoding.hashCode());
        result = prime * result + ((mimeType == null) ? 0 : mimeType.hashCode());
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true; //true
        }
        if ((!(obj instanceof MCRDataURL other)) ||
            ((charset == null && other.charset != null) || !Objects.equals(charset, other.charset)) ||
            ((data == null && other.data != null) || !MessageDigest.isEqual(data, other.data)) ||
            (encoding != other.encoding) ||
            ((mimeType == null && other.mimeType != null) || !Objects.equals(mimeType, other.mimeType))) {
            return false;
        }
        if (parameters == null) {
            return other.parameters == null;
        } else {
            return parameters.equals(other.parameters);
        }
    }
}
