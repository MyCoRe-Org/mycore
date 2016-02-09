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

    private static final Pattern PATTERN_MIMETYPE = Pattern.compile("^[a-z\\-0-9]+\\/[a-z\\-0-9]+$");

    private static final String CHARSET_PARAM = "charset";

    private static final String TOKEN_SEPARATOR = ";";

    private static final String DATA_SEPARATOR = ",";

    private static final String PARAM_SEPARATOR = "=";

    private final String mimeType;

    private final Charset charset;

    private final MCRDataURLEncoding encoding;

    private final byte[] data;

    public static final MCRDataURL parse(final String dataURL) throws MalformedURLException {
        if (dataURL.startsWith(SCHEME)) {
            String[] parts = dataURL.substring(SCHEME.length()).split(DATA_SEPARATOR, 2);
            if (parts.length == 2) {
                String[] tokens = parts[0].split(TOKEN_SEPARATOR);
                List<String> t = Arrays.stream(tokens).filter(s -> !s.contains(PARAM_SEPARATOR))
                        .collect(Collectors.toList());
                Map<String, String> p = Arrays.stream(tokens).filter(s -> s.contains(PARAM_SEPARATOR))
                        .map(s -> s.split(PARAM_SEPARATOR)).collect(Collectors.toMap(sl -> sl[0], sl -> sl[1]));

                final String mimeType = !t.isEmpty() ? t.get(0) : null;

                if (mimeType != null && !mimeType.isEmpty() && !PATTERN_MIMETYPE.matcher(mimeType).matches()) {
                    throw new MalformedURLException("Unknown mime type.");
                }

                final MCRDataURLEncoding encoding = !t.isEmpty() && t.size() > 1
                        ? MCRDataURLEncoding.fromValue(t.get(1)) : MCRDataURLEncoding.URL;

                Charset charset;
                try {
                    charset = p.containsKey(CHARSET_PARAM)
                            ? Charset.forName(decode(p.get(CHARSET_PARAM), StandardCharsets.US_ASCII))
                            : StandardCharsets.US_ASCII;
                } catch (UnsupportedEncodingException e) {
                    throw new MalformedURLException("Error decoding the charset. " + e.getMessage());
                }

                byte[] data;
                try {
                    data = encoding == MCRDataURLEncoding.BASE64 ? Base64.getDecoder().decode(parts[1])
                            : decode(parts[1], charset).getBytes(StandardCharsets.UTF_8);
                } catch (UnsupportedEncodingException e) {
                    throw new MalformedURLException("Error decoding the data. " + e.getMessage());
                }

                return new MCRDataURL(data, mimeType, charset, encoding);
            } else {
                throw new MalformedURLException("Error parse data url: " + dataURL);
            }
        } else {
            throw new MalformedURLException("Wrong protocol");
        }
    }

    /**
     * Constructs a new MCRDataURL.
     * 
     * @param data the data
     * @param mimeType the mimeType of data url
     * @param charset the charset of data url
     * @param encoding the encoding of data url
     */
    public MCRDataURL(final byte[] data, final String mimeType, final Charset charset,
            final MCRDataURLEncoding encoding) {
        this.data = data;
        this.mimeType = mimeType != null && !mimeType.isEmpty() ? mimeType : DEFAULT_MIMETYPE;
        this.charset = charset != null ? charset : StandardCharsets.US_ASCII;
        this.encoding = encoding != null ? encoding : MCRDataURLEncoding.URL;
    }

    /**
     * Constructs a new MCRDataURL.
     * 
     * @param data the data
     * @param mimeType the mimeType of data url
     * @param charset the charset of data url
     * @param encoding the encoding of data url
     */
    public MCRDataURL(final byte[] data, final String mimeType, final String charset,
            final MCRDataURLEncoding encoding) {
        this(data, mimeType, Charset.forName(charset), encoding);
    }

    /**
     * Constructs a new MCRDataURL.
     * 
     * @param data the data
     * @param mimeType the mimeType of data url
     * @param encoding the encoding of data url
     */
    public MCRDataURL(final byte[] data, final String mimeType, final MCRDataURLEncoding encoding) {
        this(data, DEFAULT_MIMETYPE, StandardCharsets.US_ASCII, encoding);
    }

    /**
     * Construct a new MCRDataURL.
     * 
     * @param data the data of data url
     * @param encoding the encoding of data url
     */
    public MCRDataURL(final byte[] data, final MCRDataURLEncoding encoding) {
        this(data, DEFAULT_MIMETYPE, StandardCharsets.US_ASCII, encoding);
    }

    /**
     * Construct a new MCRDataURL.
     * 
     * @param data the data of data url
     */
    public MCRDataURL(final byte[] data) {
        this(data, DEFAULT_MIMETYPE, StandardCharsets.US_ASCII, MCRDataURLEncoding.URL);
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
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
     * Compose MCRDataURL to string representation.
     *  
     * @return the data url as string
     * @throws MalformedURLException 
     */
    public final String compose() throws MalformedURLException {
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

        if (encoding == MCRDataURLEncoding.BASE64) {
            sb.append(TOKEN_SEPARATOR + encoding.value());
            sb.append(DATA_SEPARATOR + Base64.getEncoder().withoutPadding().encodeToString(data));
        } else {
            try {
                sb.append(DATA_SEPARATOR + encode(new String(data, StandardCharsets.UTF_8), charset));
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
        StringBuffer builder = new StringBuffer();
        builder.append("MCRDataURL [");
        if (mimeType != null) {
            builder.append("mimeType=");
            builder.append(mimeType);
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

    private static final String encode(final String str, final Charset charset) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, charset.name()).replace("+", "%20");
    }

    private static final String decode(final String str, final Charset charset) throws UnsupportedEncodingException {
        return URLDecoder.decode(str.replace("%20", "+"), charset.name());
    }
}
