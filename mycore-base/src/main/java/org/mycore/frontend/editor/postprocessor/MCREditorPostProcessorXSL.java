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

package org.mycore.frontend.editor.postprocessor;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRXSL2XMLTransformer;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCREditorPostProcessorXSL implements MCREditorPostProcessor {

    private String stylesheet;

    private MCRXSL2XMLTransformer transformer;

    public void init(Element configuration) {
        this.stylesheet = configuration.getAttributeValue("stylesheet");
        transformer = MCRXSL2XMLTransformer.getInstance("xsl/" + stylesheet);
    }

    public Document process(Document input) throws Exception {
        MCRJDOMContent source = new MCRJDOMContent(input);
        MCRContent result = transformer.transform(source);
        return result.asXML();
    }
}
