/**
 * $RCSfile: MCRNBNReservation.java,v $
 * $Revision: 1.0 $ $Date: 2003/04/17 10:16:34 $
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

package org.mycore.services.nbn;

import java.net.URLEncoder;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.frontend.editor.MCREditorXMLSource;
import org.mycore.frontend.editor.MCREditorXMLTarget;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * This servlet reserves a new NBN.
 *
 * @author Werner Greﬂhoff
 * @version $Revision: 1.0 $ $Date: 2003/04/17 10:16:34 $
 */
public class MCRNBNReservation implements MCREditorXMLSource, MCREditorXMLTarget {

	
	/** Logger */
	static Logger logger = Logger.getLogger(MCRNBNReservation.class);
	
	private static MCRConfiguration config;
	
	private static String address = null;
	private static String localcontact = null;
	private static String resolver = null;

	static {
		MCRConfiguration.instance().reload(true);
		config = MCRConfiguration.instance();
    	PropertyConfigurator.configure(config.getLoggingProperties());
    	
		try {
			address = config.getString("MCR.NBN.CorporateName.Address");
			localcontact = config.getString("MCR.NBN.localcontact");
			resolver = config.getString("MCR.NBN.TopLevelResolver");
		} catch (MCRConfigurationException mcrx) {
			String msg = "Missing configuration data.";
            logger.fatal(msg);
		}
	}
		
	/**
	 * Checks if current user is allowed to edit document with given
	 * id. This returns always true. This has to be changed for later usage scenarios!
	 *
	 * @param request the current HttpServletRequest
	 * @param context the current ServletContext
	 * @param objectID the document id to look for
	 * @return true, always
	 * @throws Exception something unusual happens
	 */ 
	public boolean isEditingAllowed(HttpServletRequest request,	ServletContext context,
			String objectID) throws Exception {
		return true;
	}

	/**
	 * Does nothing (this is not for document editing!).
	 *
	 * @param objectID the document id
	 * @param context the current ServletContext
	 * @return null
	 * @throws Exception something unusual happens
	 */ 
	public Document loadDocument(String objectID, ServletContext context)
			throws Exception {
		return null;
	}

	/**
	 * Saves the given document under given id. In this case a new
	 * NBN will be made persistent (reserved).
	 *
	 * @param object the object
	 * @param objectID the document id
	 * @param context the current ServletContext
	 * @return an url to redirect
	 * @throws Exception something unusual happens
	 */ 
	public String saveDocument(Document object,	String objectID, ServletContext context)
			throws Exception {
		Element root = object.getRootElement();

		String author = root.getChildTextTrim("author");
		String comment = root.getChildTextTrim("comment");
		MCRNBN nbn = new MCRNBN(author, comment);
		String urn = URLEncoder.encode(nbn.getNBN());
		String _urn = URLEncoder.encode(URLEncoder.encode("urn:") + urn);
		String url = "../nbn/reservation.xml?XSL.Author=" + author 
			+ "&XSL.Comment=" + URLEncoder.encode(comment) + "&XSL.Address=" + URLEncoder.encode(address)
			+ "&XSL.LocalContact=" + URLEncoder.encode(localcontact) 
			+ "&XSL.nbn=" + urn + "&XSL.Resolver=" + URLEncoder.encode(resolver)
			+ "&XSL.CodedUrn=" + _urn;
			
		return url;
	}

}
