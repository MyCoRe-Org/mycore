/**
 * $RCSfile: MCRXSLTransformation.java,v $
 * $Revision: 1.0 $ $Date: 2003/02/03 14:57:25 $
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.common.xml;

import java.io.File;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.SAXOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;

import org.mycore.common.MCRConfiguration;

/**
 * This class implements XSLTransformation functions to be used in all other
 * MyCoRe packages. The class is implemented as singleton and should be very
 * easy to use. So here is an example:
 * <PRE>
 * // Get an instance of the class
 * MCRXSLTransformation transformation = MCRXSLTransformation.getInstance();
 * 
 * // Get the template: myStylesheet could be a String (i.e. a filename),
 * // a File or a StreamSource
 * Templates templates = transformation.getStylesheet(myStylesheet);
 *
 * // Next, you are in need of a TransformerHandler:
 * TransformerHandler th = transformation.getTransformerHandler(templates);
 * 
 * // Now you are able to set some properties (if you want!):
 * Properties parameters = new Properties();
 * ...
 * setParameters(th, parameters);
 * 
 * // Finally, you need an OutputStream and might get at work:
 * OutputStream out = response.getOutputStream();
 * transformation.transform(jdom, th, out);
 * 
 * // You might also want to transform into something different,
 * // perhaps a ZIP-File:
 * OutputStream out = new ZipOutputStream(response.getOutputStream());
 * ((ZipOutputStream) out).setLevel(Deflater.BEST_COMPRESSION);
 * ZipEntry ze = new ZipEntry("_index.htm");
 * ((ZipOutputStream) out).putNextEntry(ze);
 * ...
 * 
 * // After all this work is done, you could close the OutputStream:
 * out.close();
 * // This is not done by <CODE>transform</CODE>, the later example
 * // should show, why.
 * </PRE>
 * 
 * @author Werner Gresshoff
 *
 * @version $Revision: 1.0 $ $Date: 2003/02/03 14:57:25 $
 **/
public class MCRXSLTransformation {

	private static Logger logger = null;
	
	private static SAXTransformerFactory factory = null;  

	private static MCRXSLTransformation singleton = null;
	
	/**
	 * Method MCRXSLTransformation.
	 */
	private MCRXSLTransformation() {
		logger = Logger.getLogger(MCRXSLTransformation.class);
    	MCRConfiguration.instance().reload(true);
    	PropertyConfigurator.configure(MCRConfiguration.instance().getLoggingProperties());
    	
    	try {
	    	TransformerFactory tf = TransformerFactory.newInstance();
	    	if (tf.getFeature(SAXTransformerFactory.FEATURE)) {
    	  	  	factory = (SAXTransformerFactory) tf;
    		} else {
    			logger.fatal("TransformerFactory could not be initialized.");
	    	}
    	} catch (TransformerFactoryConfigurationError tfce) {
    		if (tfce.getMessage() != null) {
    			logger.fatal(tfce.getMessage());
    		} else {
    			logger.fatal("Error in TranformerFactory configuration.");
    		}
    	}
	}
	
	/**
	 * Method getInstance. Creates an instance of MCRXSLTransformation, when called the first time.
	 * @return MCRXSLTransformation
	 */
	public static synchronized MCRXSLTransformation getInstance() {
		if (singleton == null) {
			singleton = new MCRXSLTransformation();
		}
		
		return singleton;
	}
	
	/**
	 * Method getStylesheet. Returns a precompiled stylesheet.
	 * @param stylesheet Full path to the stylesheet
	 * @return Templates The precompiled Stylesheet
	 */
	public Templates getStylesheet(String stylesheet) {
	    File styleFile = new File(stylesheet);
	    
		if (!styleFile.exists()) {
			logger.fatal("The Stylesheet doesn't exist.");
			return null;
		}
		return getStylesheet(styleFile);
	}
	
	/**
	 * Method getStylesheet. Returns a precompiled stylesheet.
	 * @param stylesheet A File with the stylesheet code
	 * @return Templates The precompiled Stylesheet
	 */
	public Templates getStylesheet(File stylesheet) {
		return getStylesheet(new StreamSource(stylesheet));
	}
	
	/**
	 * Method getStylesheet. Returns a precompiled stylesheet.
	 * @param stylesheet A StreamSource
	 * @return Templates The precompiled Stylesheet
	 */
	public Templates getStylesheet(StreamSource stylesheet) {
		try {
			Templates out = factory.newTemplates(stylesheet);
			return out;
		} catch (TransformerConfigurationException tcx) {
			logger.fatal(tcx.getMessageAndLocation());
			return null;
		}
	}
	
	/**
	 * Method getTransformerHandler. Returns a TransformerHandler for the given Template.
	 * @param stylesheet
	 * @return TransformerHandler
	 */
	public TransformerHandler getTransformerHandler(Templates stylesheet) {
		try {
	        TransformerHandler handler = factory.newTransformerHandler(stylesheet);
			return handler;
		} catch (TransformerConfigurationException tcx) {
			logger.fatal(tcx.getMessageAndLocation());
			return null;
		}
	}
	
	/**
	 * Method setParameters. Set some parameters which can be used by the Stylesheet for the transformation.
	 * @param handler
	 * @param parameters
	 */
	public void setParameters(TransformerHandler handler, Properties parameters) {
		Transformer transformer = handler.getTransformer();
		Enumeration names = parameters.propertyNames();
    
		while (names.hasMoreElements()) {
			String name = (String) (names.nextElement());
			String value = parameters.getProperty(name);

			transformer.setParameter(name, value);
		}
	}
	
	/**
	 * Method transform. Transforms a JDOM-Document to the given OutputStream
	 * @param in
	 * @param handler
	 * @param out
	 */
	public void transform(org.jdom.Document in, TransformerHandler handler, OutputStream out) {
	    handler.setResult(new StreamResult(out));
    
    	try {
    		new SAXOutputter(handler).output(in); 
    	} catch (JDOMException ex) {
    		logger.error("Error while transforming an XML document with an XSL stylesheet.");
	    }
	}
	
	/**
	 * Method transform. Transforms a JDOM-Document <i>in</i> with a given <i>stylesheet</i> to a new document.
	 * @param in A JDOM-Document.
	 * @param stylesheet The Filename with complete path (this is not a servlet!) of the stylesheet.
	 * @return Document The new document or null, if an exception was thrown.
	 */
    public static org.jdom.Document transform(org.jdom.Document in, String stylesheet) {
        try {
            JDOMResult out = new JDOMResult();
            Transformer transformer = TransformerFactory.newInstance().newTransformer(
                    new StreamSource(new File(stylesheet)));
            transformer.transform(new JDOMSource(in), out);
            
            return out.getDocument();
        }
        catch (TransformerException e) {
            logger.fatal(e.getMessage());
            return null;
        }
    }

}
