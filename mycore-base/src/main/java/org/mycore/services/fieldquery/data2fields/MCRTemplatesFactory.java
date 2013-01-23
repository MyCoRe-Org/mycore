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

package org.mycore.services.fieldquery.data2fields;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.log4j.Logger;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfigurationException;
import org.mycore.services.fieldquery.MCRFieldDef;

public class MCRTemplatesFactory {

    private final static Logger LOGGER = Logger.getLogger(MCRTemplatesFactory.class);

    private static MCRCache cache = new MCRCache(50, MCRTemplatesFactory.class.getName());

    public static Templates getTemplates(MCRFieldsSelector selector) {
        MCRRelevantFields fields = MCRRelevantFields.getFieldsFor(selector);
        String key = fields.getKey();

        Templates templates = (Templates) (cache.get(key));
        if (templates == null) {
            MCRXSLBuilder xsl = buildTemplates(fields);
            templates = compile(xsl);
            cache.put(key, templates);
        }

        return templates;
    }

    private static MCRXSLBuilder buildTemplates(MCRRelevantFields fields) {
        MCRXSLBuilder xsl = new MCRXSLBuilder();

        for (MCRFieldDef field : fields.getFields())
            xsl.addXSLForField(field);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("========== XSL Stylesheet for " + fields.getKey() + " ==========");
            LOGGER.debug("\n" + xsl + "\n");
        }

        return xsl;
    }

    private static Templates compile(MCRXSLBuilder xsl) {
        try {
            Source source = new JDOMSource(xsl.getStylesheet());
            return MCRData2FieldsXML.factory.newTemplates(source);
        } catch (TransformerConfigurationException exc) {
            String msg = "Error while compiling XSL stylesheet: " + exc.getMessageAndLocation();
            throw new MCRConfigurationException(msg, exc);
        }
    }

}
