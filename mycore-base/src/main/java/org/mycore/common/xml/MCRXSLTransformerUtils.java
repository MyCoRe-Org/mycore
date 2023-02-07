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

package org.mycore.common.xml;

import org.mycore.common.config.MCRConfiguration2;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import java.util.Map;
import java.util.Properties;

public class MCRXSLTransformerUtils {

    private static final Map<String, String> MIME_TYPE_EXTENSIONS;

    private static final Map<String, String> METHOD_EXTENSIONS;

    static {
        MIME_TYPE_EXTENSIONS = MCRConfiguration2.getSubPropertiesMap("MCR.XMLUtils.FileExtension.MimeType.");
        METHOD_EXTENSIONS = MCRConfiguration2.getSubPropertiesMap("MCR.XMLUtils.FileExtension.Method.");
    }

    /**
     * Returns the preferred file extension for a transformer based on the transformers output properties.
     * This will inspect, in this order, the output properties <code>media-type</code> and <code>method</code> and
     * return the first associated file extension, or the provided fallback, if no such file extension is found.
     * <br/>
     * To look up an associated file extension, the configuration keys
     * <code>MCR.XMLUtils.FileExtension.MimeType.{$mimeType}</code> and
     * <code>MCR.XMLUtils.FileExtension.Method.{$method}</code>, respectively are used.
     *
     * @param transformer the transformer
     * @param fallback the fallback
     * @return the file extension
     */
    public static String getFileExtension(Transformer transformer, String fallback) {
        return getFileExtension(transformer.getOutputProperties(), fallback);
    }

    /**
     * Returns the preferred file extension for a transformers output properties.
     * This will inspect, in this order, the output properties <code>media-type</code> and <code>method</code> and
     * return the first associated file extension, or the provided fallback, if no such file extension is found.
     * <br/>
     * To look up an associated file extension, the configuration keys
     * <code>MCR.XMLUtils.FileExtension.MimeType.{$mimeType}</code> and
     * <code>MCR.XMLUtils.FileExtension.Method.{$method}</code>, respectively are used.
     *
     * @param outputProperties the output properties
     * @param fallback the fallback
     * @return the file extension
     */
    public static String getFileExtension(Properties outputProperties, String fallback) {
        String mimeType = outputProperties.getProperty(OutputKeys.MEDIA_TYPE);
        if (mimeType != null) {
            String mimeTypeExtension = MIME_TYPE_EXTENSIONS.get(mimeType);
            if (mimeTypeExtension != null) {
                return mimeTypeExtension;
            }
        }
        String method = outputProperties.getProperty(OutputKeys.METHOD);
        if (method != null) {
            String methodExtension = METHOD_EXTENSIONS.get(method);
            if (methodExtension != null) {
                return methodExtension;
            }
        }
        return fallback;
    }


}
