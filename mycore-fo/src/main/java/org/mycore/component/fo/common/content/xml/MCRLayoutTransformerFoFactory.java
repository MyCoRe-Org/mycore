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

package org.mycore.component.fo.common.content.xml;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.content.transformer.MCRIdentityTransformer;
import org.mycore.common.content.transformer.MCRTransformerPipe;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.common.xml.MCRLayoutTransformerFactory;
import org.mycore.component.fo.common.content.transformer.MCRFopper;

import com.google.common.collect.Lists;
import com.google.common.net.MediaType;

/**
 * This class acts as a {@link org.mycore.common.content.transformer.MCRContentTransformer} factory for 
 * {@link org.mycore.common.xml.MCRLayoutService}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRLayoutTransformerFoFactory extends MCRLayoutTransformerFactory {
    /** Map of transformer instances by ID */
    private static HashMap<String, MCRContentTransformer> transformers = new HashMap<>();

    private static Logger LOGGER = LogManager.getLogger(MCRLayoutTransformerFoFactory.class);

    private static MCRFopper fopper = new MCRFopper();

    private static final MCRIdentityTransformer NOOP_TRANSFORMER = new MCRIdentityTransformer("text/xml", "xml");

    /**
     * Returns the transformer with the given ID. If the transformer is not instantiated yet,
     * it is created and initialized.
     */
    @Override
    public MCRContentTransformer getTransformer(String id) throws Exception {
        MCRContentTransformer transformer = transformers.get(id);
        if (transformer != null) {
            return transformer;
        }
        //try to get configured transformer
        transformer = MCRContentTransformerFactory.getTransformer(id.replaceAll("-default$", ""));
        if (transformer != null) {
            transformers.put(id, transformer);
            return transformer;
        }
        return buildLayoutTransformer(id);
    }

    private static MCRContentTransformer buildLayoutTransformer(String id) throws Exception {
        String idStripped = id.replaceAll("-default$", "");
        LOGGER.debug("Configure property MCR.ContentTransformer.{}.Class if you do not want to use default behaviour.",
            idStripped);
        String stylesheet = getResourceName(id);
        if (stylesheet == null) {
            LOGGER.debug("Using noop transformer for {}", idStripped);
            return NOOP_TRANSFORMER;
        }
        String[] stylesheets = getStylesheets(idStripped, stylesheet);
        MCRContentTransformer transformer = MCRXSLTransformer.getInstance(stylesheets);
        String mimeType = transformer.getMimeType();
        if (isPDF(mimeType)) {
            transformer = new MCRTransformerPipe(transformer, fopper);
            LOGGER.debug("Using stylesheet '{}' for {} and MCRFopper for PDF output.", Lists.newArrayList(stylesheets),
                idStripped);
        } else {
            LOGGER.debug("Using stylesheet '{}' for {}", Lists.newArrayList(stylesheets), idStripped);
        }
        transformers.put(id, transformer);
        return transformer;
    }

    private static boolean isPDF(String mimeType) {
        return MediaType.parse(mimeType).is(MediaType.PDF);
    }

}
