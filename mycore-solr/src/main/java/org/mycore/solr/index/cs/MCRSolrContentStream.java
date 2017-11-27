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

package org.mycore.solr.index.cs;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.datamodel.metadata.MCRBase;

/**
 * Content stream suitable for wrapping {@link MCRBase} and {@link Document} objects.
 * 
 * @author shermann
 * @author Matthias Eichner
 */
public class MCRSolrContentStream extends MCRSolrAbstractContentStream<MCRContent> {

    static final Logger LOGGER = LogManager.getLogger(MCRSolrContentStream.class);

    private static final MCRContentTransformer TRANSFORMER;

    static {
        String transformerId = MCRConfiguration.instance().getString(
            CONFIG_PREFIX + "IndexHandler.ContentStream.Transformer");
        TRANSFORMER = MCRContentTransformerFactory.getTransformer(transformerId);
    }

    public MCRSolrContentStream(String id, MCRContent content) {
        super(content);
        this.setName(id);
    }

    @Override
    protected void setup() throws IOException {
        MCRContent content = getSource();
        ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
        getTransformer().transform(content, out);
        byte[] byteArray = out.toByteArray();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(new String(byteArray, StandardCharsets.UTF_8));
        }
        this.setSourceInfo(content.getSystemId());
        try {
            this.setContentType(getTransformer().getMimeType());
        } catch (Exception e) {
            Exception unwrapExCeption = MCRUtils.unwrapExCeption(e, IOException.class);
            if (unwrapExCeption instanceof IOException) {
                throw (IOException) unwrapExCeption;
            }
            throw new IOException(e);
        }
        this.setSize((long) byteArray.length);
        this.setInputStream(new ByteArrayInputStream(byteArray));
    }

    public static MCRContentTransformer getTransformer() {
        return TRANSFORMER;
    }

    @Override
    protected Charset getCharset() {
        String encoding = super.source.getEncoding();
        return encoding == null ? null : Charset.forName(encoding);
    }

}
