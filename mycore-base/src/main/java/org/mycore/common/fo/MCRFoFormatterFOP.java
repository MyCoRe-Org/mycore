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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXResult;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.apps.io.ResourceResolverFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlgraphics.io.Resource;
import org.apache.xmlgraphics.io.ResourceResolver;
import org.mycore.common.MCRCoreVersion;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xsl.MCRErrorListener;
import org.xml.sax.SAXException;

/**
 * This class implements the interface to use configured XSL-FO formatters for the layout service.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision: 1.8 $ $Date: 2008/05/28 13:43:31 $
 */

public class MCRFoFormatterFOP implements MCRFoFormatterInterface {

    private final static Logger LOGGER = LogManager.getLogger();

    private FopFactory fopFactory;

    ResourceResolver resolver = MCRURIResolver.getServletContext() == null
        ? ResourceResolverFactory.createDefaultResourceResolver() : new ResourceResolver() {
            public OutputStream getOutputStream(URI uri) throws IOException {
                URL url = MCRURIResolver.getServletContext().getResource(uri.toASCIIString());
                return url.openConnection().getOutputStream();
            }

            public Resource getResource(URI uri) throws IOException {
                return new Resource(MCRURIResolver.getServletContext().getResourceAsStream(uri.toASCIIString()));
            }
        };

    /**
     * Protected constructor to create the singleton instance
     */
    public MCRFoFormatterFOP() {
        FopFactoryBuilder fopFactoryBuilder = new FopFactoryBuilder(new File(".").toURI(), resolver);
        String fo_cfg = MCRConfiguration.instance().getString("MCR.LayoutService.FoFormatter.FOP.config", "");
        if (!fo_cfg.isEmpty()) {
            try {
                DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
                Configuration cfg = cfgBuilder.build(getClass().getClassLoader().getResource(fo_cfg).toString());
                fopFactoryBuilder.setConfiguration(cfg);
            } catch (ConfigurationException | SAXException | IOException e) {
                LOGGER.error("Exception while loading FOP configuration from {}.", fo_cfg, e);
            }
        }
        fopFactory = fopFactoryBuilder.build();
        getTransformerFactory();
    }

    private static TransformerFactory getTransformerFactory() throws TransformerFactoryConfigurationError {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setURIResolver(MCRURIResolver.instance());
        transformerFactory.setErrorListener(MCRErrorListener.getInstance());
        return transformerFactory;
    }

    @Override
    @Deprecated
    public final void transform(InputStream in_stream, OutputStream out) throws TransformerException, IOException {
        transform(new MCRStreamContent(in_stream), out);
    }

    @Override
    public void transform(MCRContent input, OutputStream out) throws TransformerException, IOException {
        try {
            FOUserAgent userAgent = fopFactory.newFOUserAgent();
            userAgent.setProducer(MessageFormat.format("MyCoRe {0} ({1})", MCRCoreVersion.getCompleteVersion(), userAgent.getProducer()));
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, userAgent, out);
            Source src = input.getSource();
            Result res = new SAXResult(fop.getDefaultHandler());
            Transformer transformer = getTransformerFactory().newTransformer();
            transformer.transform(src, res);
        } catch (FOPException e) {
            throw new TransformerException(e);
        } finally {
            out.close();
        }
    }
}
