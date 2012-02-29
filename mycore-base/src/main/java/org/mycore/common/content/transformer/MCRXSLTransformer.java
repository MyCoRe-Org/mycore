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

import java.io.ByteArrayOutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.xsl.MCRTemplatesSource;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.common.xsl.MCRXSLTransformerFactory;

/**
 * Transforms XML content using a static XSL stylesheet.
 * The stylesheet is configured via
 * 
 * MCR.ContentTransformer.{ID}.Stylesheet

 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXSLTransformer extends MCRContentTransformer {

    /** The compiled XSL stylesheet */
    private MCRTemplatesSource templates;

    @Override
    public void init(String id) {
        super.init(id);
        String property = "MCR.ContentTransformer." + id + ".Stylesheet";
        String stylesheet = MCRConfiguration.instance().getString(property);
        this.templates = new MCRTemplatesSource(stylesheet);
    }

    @Override
    public MCRContent transform(MCRContent source) throws Exception {
        Transformer transformer = MCRXSLTransformerFactory.getTransformer(templates);
        new MCRParameterCollector(null).setParametersTo(transformer);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(baos);
        transformer.transform(source.getSource(), result);
        baos.close();
        return new MCRByteContent(baos.toByteArray());
    }
}
