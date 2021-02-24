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

package org.mycore.frontend.xeditor;

import javax.xml.transform.TransformerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;

/**
 * PostProcessor for MyCoRe editor framework
 * that allows execution of XSLT3 stylesheets after an editor is closed
 */
public class MCRPostProcessorXSL3 extends MCRPostProcessorXSL {

    private static final Logger LOGGER = LogManager.getLogger(MCRPostProcessorXSL3.class);

    public MCRPostProcessorXSL3() throws ClassNotFoundException {
        try {
            Class<? extends TransformerFactory> factoryClass = MCRClassTools
                .forName("net.sf.saxon.TransformerFactoryImpl");
            init(factoryClass);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Transformer class not found", e);
        }
    }
}
