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

package org.mycore.pandoc;

import java.io.IOException;

import org.jdom2.Element;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;

/**
 * Generic transformer using Pandoc
 *
 * @author Kai Brandhorst
 */
public class MCRPandocTransformer extends MCRContentTransformer {

    private String inputFormat;
    private String outputFormat;

    @Override
    public void init(String id) {
        super.init(id);
        inputFormat = MCRConfiguration2.getStringOrThrow("MCR.ContentTransformer." + id + ".InputFormat");
        outputFormat = MCRConfiguration2.getStringOrThrow("MCR.ContentTransformer." + id + ".OutputFormat");
    }

    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        try {
            Element pandoc = MCRPandocAPI.convertToXML(source.asString(), inputFormat, outputFormat);
            if(!pandoc.getChildren().isEmpty()) {
                pandoc = pandoc.getChildren().get(0).detach();
            }
            return new MCRJDOMContent(pandoc);
        } catch (Exception ex) {
            String msg = "Exception transforming from " + inputFormat + " to " + outputFormat + " via Pandoc";
            throw new IOException(msg, ex);
        }
    }
}
