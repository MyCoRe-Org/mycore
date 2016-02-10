/*
 * $Id$ 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */
package org.mycore.datamodel.common;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents the data URL scheme (RFC2397).
 * 
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class MCRDataURL implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String SCHEME = "data:";

    private static final String DEFAULT_MIMETYPE = "text/plain";

    private static final Pattern PATTERN_MIMETYPE = Pattern.compile("^[a-z0-9\\-\\+]+\\/[a-z0-9\\-\\+]+$");

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
     * Unserialize a {@link String} to {@link MCRDataURL}.
     * 
     * @param dataURL the data url string
     * @return a {@link MCRDataURL} object
     * @throws MalformedURLException
     */
    public static final MCRDataURL unserialize(final String dataURL) throws MalformedURLException {
        final String url = dataURL.trim();
        if (url.startsWith(SCHEME)) {
            String[] parts = url.substring(SCHEME.length()).split(DATA_SEPARATOR, 2);
            if (parts.length == 2) {
                String[] tokens = parts[0].split(TOKEN_SEPARATOR);
                List<String> token = Arrays.stream(tokens).filter(s -> !s.contains(PARAM_SEPARATOR))
                        .collect(Collectors.toList());
                Map<String, String> params = Arrays.stream(tokens).filter(s -> s.contains(PARAM_SEPARATOR))
                        .map(s -> s.split(PARAM_SEPARATOR, 2)).collect(Collectors.toMap(sl -> sl[0], sl -> {
                            try {
                                return decode(sl[1], StandardCharsets.UTF_8);
                            } catch (Exception e) {
                                throw new RuntimeException("Error encoding the parameter value \"" + sl[1]
                                        + "\". Error: " + e.getMessage());
                            }
                        }));

                final String mimeType = !token.isEmpty() ? token.get(0) : null;

                if (mimeType != null && !mimeType.isEmpty() && !PATTERN_MIMETYPE.matcher(mimeType).matches()) {
                    throw new MalformedURLException("Unknown mime type.");
                }

                final MCRDataURLEncoding encoding;
                try {
                    encoding = !token.isEmpty() && token.size() > 1 ? MCRDataURLEncoding.fromValue(token.get(1))
                            : MCRDataURLEncoding.URL;
                } catch (IllegalArgumentException e) {
                    throw new MalformedURLException("Unknown encoding.");
                }

                Charset charset = params.containsKey(CHARSET_PARAM) ? Charset.forName(params.get(CHARSET_PARAM))
                        : StandardCharsets.US_ASCII;

                byte[] data;
                try {
                    data = encoding == MCRDataURLEncoding.BASE64 ? Base64.getDecoder().decode(parts[1])
                            : decode(parts[1], charset).getBytes(StandardCharsets.UTF_8);
                } catch (IllegalArgumentException | UnsupportedEncodingException e) {
                    throw new MalformedURLException("Error decoding the data. " + e.getMessage());
                }

                return new MCRDataURL(data, encoding, mimeType, params);
            } else {
                throw new MalformedURLException("Error parse data url: " + url);
            }
        } else {
            throw new MalformedURLException("Wrong protocol");
        }

    }

    /**
     * Constructs a new {@link MCRDataURL}.
     * 
     * @param data the data
     * @param encoding the encoding of data url
     * @param mimeType the mimeType of data url
     * @param parameters a list of paramters of data url
     */
    public MCRDataURL(final byte[] data, final MCRDataURLEncoding encoding, final String mimeType,
            final Map<String, String> parameters) {
        this.data = data;
        this.encoding = encoding != null ? encoding : MCRDataURLEncoding.URL;
        this.mimeType = mimeType != null && !mimeType.isEmpty() ? mimeType : DEFAULT_MIMETYPE;

        if (parameters != null) {
            this.parameters = Collections.unmodifiableMap(new LinkedHashMap<String, String>(
                    parameters.entrySet().stream().filter(e -> !CHARSET_PARAM.equals(e.getKey()))
                            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()))));
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
            final Charset charset) {
        this.data = data;
        this.encoding = encoding != null ? encoding : MCRDataURLEncoding.URL;
        this.mimeType = mimeType != null && !mimeType.isEmpty() ? mimeType : DEFAULT_MIMETYPE;
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
    public MCRDataURL(final byte[] data, final MCRDataURLEncoding encoding, final String mimeType,
            final String charset) {
        this(data, encoding, mimeType, Charset.forName(charset));
    }

    /**
     * Constructs a new {@link MCRDataURL}.
     * 
     * @param data the data
     * @param encoding the encoding of data url
     * @param mimeType the mimeType of data url
     */
    public MCRDataURL(final byte[] data, final MCRDataURLEncoding encoding, final String mimeType) {
        this(data, encoding, DEFAULT_MIMETYPE, StandardCharsets.US_ASCII);
    }

    /**
     * Constructs a new {@link MCRDataURL}.
     * 
     * @param data the data of data url
     * @param encoding the encoding of data url
     */
    public MCRDataURL(final byte[] data, final MCRDataURLEncoding encoding) {
        this(data, encoding, DEFAULT_MIMETYPE, StandardCharsets.US_ASCII);
    }

    /**
     * Constructs a new {@link MCRDataURL}.
     * 
     * @param data the data of data url
     */
    public MCRDataURL(final byte[] data) {
        this(data, MCRDataURLEncoding.URL, DEFAULT_MIMETYPE, StandardCharsets.US_ASCII);
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
        return data;
    }

    /**
     * Serialize a {@link MCRDataURL} to {@link String}.
     *  
     * @return the data url as string
     * @throws MalformedURLException 
     */
    public final String serialize() throws MalformedURLException {
        StringBuffer sb = new StringBuffer(SCHEME);

        if (!PATTERN_MIMETYPE.matcher(mimeType).matches()) {
            throw new MalformedURLException("Unknown mime type.");
        }

        if (!DEFAULT_MIMETYPE.equals(mimeType) || charset != StandardCharsets.US_ASCII) {
            sb.append(mimeType);
        }

        if (charset != StandardCharsets.US_ASCII) {
            sb.append(TOKEN_SEPARATOR + CHARSET_PARAM + PARAM_SEPARATOR + charset.name());
        }

        parameters.entrySet().forEach(p -> {
            try {
                sb.append(
                        TOKEN_SEPARATOR + p.getKey() + PARAM_SEPARATOR + encode(p.getValue(), StandardCharsets.UTF_8));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(
                        "Error encoding the parameter value \"" + p.getValue() + "\". Error: " + e.getMessage());
            }
        });

        if (encoding == MCRDataURLEncoding.BASE64) {
            sb.append(TOKEN_SEPARATOR + encoding.value());
            sb.append(DATA_SEPARATOR + Base64.getEncoder().withoutPadding().encodeToString(data));
        } else {
            try {
                sb.append(DATA_SEPARATOR + encode(new String(data, charset), charset));
            } catch (UnsupportedEncodingException e) {
                throw new MalformedURLException("Error encoding the data. Error: " + e.getMessage());
            }
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
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((encoding == null) ? 0 : encoding.hashCode());
        result = prime * result + ((mimeType == null) ? 0 : mimeType.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MCRDataURL)) {
            return false;
        }
        MCRDataURL other = (MCRDataURL) obj;
        if (charset == null) {
            if (other.charset != null) {
                return false;
            }
        } else if (!charset.equals(other.charset)) {
            return false;
        }
        if (data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!data.equals(other.data)) {
            return false;
        }
        if (encoding != other.encoding) {
            return false;
        }
        if (mimeType == null) {
            if (other.mimeType != null) {
                return false;
            }
        } else if (!mimeType.equals(other.mimeType)) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("MCRDataURL [");
        if (mimeType != null) {
            builder.append("mimeType=");
            builder.append(mimeType);
            builder.append(", ");
        }
        if (parameters != null) {
            builder.append("parameters=");
            builder.append(toString(parameters.entrySet(), maxLen));
            builder.append(", ");
        }
        if (charset != null) {
            builder.append("charset=");
            builder.append(charset);
            builder.append(", ");
        }
        if (encoding != null) {
            builder.append("encoding=");
            builder.append(encoding);
            builder.append(", ");
        }
        if (data != null) {
            builder.append("data.length=");
            builder.append(data.length);
        }
        builder.append("]");
        return builder.toString();
    }

    private String toString(Collection<?> collection, int maxLen) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int i = 0;
        for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(iterator.next());
        }
        builder.append("]");
        return builder.toString();
    }

    private static final String encode(final String str, final Charset charset) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, charset.name()).replace("+", "%20");
    }

    private static final String decode(final String str, final Charset charset) throws UnsupportedEncodingException {
        return URLDecoder.decode(str.replace("%20", "+"), charset.name());
    }
}
