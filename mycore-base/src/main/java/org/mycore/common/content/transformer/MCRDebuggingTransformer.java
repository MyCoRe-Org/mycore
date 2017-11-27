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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRContent;

/**
 * Does not really transform content, instead logs the current contents.
 * This can be useful when multiple transformers are combined in a pipe. 
 *  
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRDebuggingTransformer extends MCRContentTransformer {

    private static final Logger LOGGER = LogManager.getLogger(MCRDebuggingTransformer.class);

    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        if (LOGGER.isDebugEnabled()) {
            if (!source.isReusable())
                source = source.getReusableCopy();

            LOGGER.debug(">>>>>>>>>>>>>>>>>>>>");
            LOGGER.debug(source.asString());
            LOGGER.debug("<<<<<<<<<<<<<<<<<<<<");
        }
        return source;
    }
}
