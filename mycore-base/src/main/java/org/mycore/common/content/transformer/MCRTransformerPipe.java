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

package org.mycore.common.content.transformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.xsl.MCRParameterCollector;

/**
 * Transforms MCRContent by using a pipe of multiple transformers.
 * The transformers to execute are configured by giving a list of their IDs, for example
 * 
 * MCR.ContentTransformer.{ID}.Steps=ID2 ID3
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRTransformerPipe extends MCRParameterizedTransformer {

    /** List of transformers to execute */
    private List<MCRContentTransformer> transformers = new ArrayList<>();

    public MCRTransformerPipe(MCRContentTransformer... transformers) {
        this();
        this.transformers.addAll(Arrays.asList(transformers));
    }

    /* needed for MCRConfiguration.getInstanceOf() to work */
    public MCRTransformerPipe() {
        super();
    }

    @Override
    public void init(String id) {
        String steps = MCRConfiguration.instance().getString("MCR.ContentTransformer." + id + ".Steps");
        StringTokenizer tokens = new StringTokenizer(steps, " ;,");
        while (tokens.hasMoreTokens()) {
            String transformerID = tokens.nextToken();
            MCRContentTransformer transformer = MCRContentTransformerFactory.getTransformer(transformerID);
            if (transformer == null) {
                throw new MCRConfigurationException(
                    "Transformer pipe element '" + transformerID + "' is not configured.");
            }
            transformers.add(transformer);
        }
    }

    @Override
    public MCRContent transform(MCRContent content) throws IOException {
        return transform(content, new MCRParameterCollector());
    }

    @Override
    public String getMimeType() throws Exception {
        return transformers.get(transformers.size() - 1).getMimeType();
    }

    @Override
    public String getEncoding() throws Exception {
        return transformers.get(transformers.size() - 1).getEncoding();
    }

    @Override
    protected String getDefaultExtension() {
        return transformers.get(transformers.size() - 1).getDefaultExtension();
    }

    @Override
    public String getFileExtension() throws Exception {
        return transformers.get(transformers.size() - 1).getFileExtension();
    }

    @Override
    public MCRContent transform(MCRContent source, MCRParameterCollector parameter) throws IOException {
        for (MCRContentTransformer transformer : transformers) {
            if (transformer instanceof MCRParameterizedTransformer) {
                source = ((MCRParameterizedTransformer) transformer).transform(source, parameter);
            } else {
                source = transformer.transform(source);
            }
        }
        return source;
    }
}
