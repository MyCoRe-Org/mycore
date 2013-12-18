/**
 * $Revision: 1.8 $ $Date: 2008/05/28 13:43:31 $
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

package org.mycore.common.fo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRURIResolver;
import org.xml.sax.SAXException;

/**
 * This class implements the interface to use configured XSL-FO formatters for the layout service.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision: 1.8 $ $Date: 2008/05/28 13:43:31 $
 */

public class MCRFoFormatterFOP implements MCRFoFormatterInterface {

    private final static Logger LOGGER = Logger.getLogger(MCRFoFormatterFOP.class);

    private FopFactory fopFactory;

    /**
     * Protected constructor to create the singleton instance
     * @throws IOException 
     */
    public MCRFoFormatterFOP() throws Exception {
        fopFactory = FopFactory.newInstance();
        fopFactory.setURIResolver(MCRURIResolver.instance());
        String fo_cfg = MCRConfiguration.instance().getString("MCR.LayoutService.FoFormatter.FOP.config",null);
        if ((fo_cfg != null) && (fo_cfg.length() != 0)) {
            DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
            Configuration cfg = null;
            try {
                cfg = cfgBuilder.build(MCRFoFormatterFOP.class.getResourceAsStream("/" + fo_cfg));
            } catch (ConfigurationException e) {
                LOGGER.error("ConfigurationException - Can't use FOP configuration for " + fo_cfg);
                LOGGER.error(e.getMessage());
            } catch (SAXException e) {
                LOGGER.error("SAXException - Can't parse FOP configuration for " + fo_cfg);
                LOGGER.error(e.getMessage());
            } catch (IOException e) {
                LOGGER.error("IOException - Can't find FOP configuration for " + fo_cfg);
                LOGGER.error(e.getMessage());
            }
            if (cfg != null) {
                try {
                    fopFactory.setUserConfig(cfg);
                } catch (FOPException e) {
                    LOGGER.error("FOPException - Error while setting FOP configuration for " + fo_cfg);
                    LOGGER.error(e.getMessage());
                }
            }
        }
    }

    public final void transform(InputStream in_stream, OutputStream out) throws TransformerException {
        try {
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);
            Source src = new StreamSource(in_stream);
            Result res = new SAXResult(fop.getDefaultHandler());
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(src, res);
        } catch (FOPException e) {
            throw new TransformerException(e);
        }
    }
}
