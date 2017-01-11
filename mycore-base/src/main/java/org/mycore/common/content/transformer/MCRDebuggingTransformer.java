/*
 * $Revision$ 
 * $Date$
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

    private final static Logger LOGGER = LogManager.getLogger(MCRDebuggingTransformer.class);

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
