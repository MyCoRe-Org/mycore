/**
 * $RCSfile: MCRNBNRegistration.java,v $
 * $Revision: 1.0 $ $Date: 2003/04/22 15:35:41 $
 *
 * Copyright (C) 2000-2002 University of Essen, Germany
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
 * along with this program, normally in the file sources/gpl.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */

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
 * This class implements the MCREditorXMLSource and the MCREditorXMLTarget
 * and registrates a NBN (optionally with the DDB).
 *
 * @author Werner Greﬂhoff
 *
 * @version $Revision: 1.0 $ $Date: 2003/04/22 15:35:41 $
 */
public class MCRNBNRegistration
	implements MCREditorXMLSource, MCREditorXMLTarget {
		
	/** Logger */
	static Logger logger = Logger.getLogger(MCRNBNRegistration.class);
	
	private static MCRConfiguration config;

	static {
		MCRConfiguration.instance().reload(true);
		config = MCRConfiguration.instance();
    	PropertyConfigurator.configure(config.getLoggingProperties());
	}

	/**
	 * @see org.mycore.frontend.editor.MCREditorXMLTarget#isEditingAllowed(HttpServletRequest, ServletContext, String)
	 */
	public boolean isEditingAllowed(HttpServletRequest request,	ServletContext context,	String objectID)
			throws Exception {
		return true;
	}

	/**
	 * @see org.mycore.frontend.editor.MCREditorXMLSource#loadDocument(String, ServletContext)
	 */
	public Document loadDocument(String objectID, ServletContext context)
			throws Exception {
		return null;
	}

	/**
	 * @see org.mycore.frontend.editor.MCREditorXMLTarget#saveDocument(Document, String, ServletContext)
	 */
	public String saveDocument(Document object,	String objectID, ServletContext context)
			throws Exception {
		Element root = object.getRootElement();
		String urn;
		MCRNBN nbn = null;
		Boolean newNBN = new Boolean("true".equals(root.getChild("nbn").getAttributeValue("new")));
		Boolean registerNBN = new Boolean("true".equals(root.getChild("nbn").getAttributeValue("register")));
		
		logger.debug("newNBN: " + newNBN.toString());
		logger.debug("registerNBN: " + registerNBN.toString());
		
		if (!newNBN.booleanValue()) {
			urn = root.getChildTextTrim("nbn");
			logger.info("NBN: " + urn);
			try {
				long lUrn = Long.parseLong(urn);
				if (MCRNBN.exists(MCRNBN.getLocalPrefix() + urn)) {
					nbn = new MCRNBN(MCRNBN.getLocalPrefix() + urn);
				} else {
					logger.debug("NBN nicht gefunden: " + MCRNBN.getLocalPrefix() + urn);
					// Fehler
				}
			} catch (NumberFormatException nfx) {
			}
		} else {
			nbn = new MCRNBN();
			urn = nbn.getNISSandChecksum();
		}
		
		String url = null;
		if (!registerNBN.booleanValue()) {
			url = "../servlets/NBNRegistration/Document-" + URLEncoder.encode(objectID) + ".noddb?urn=" + URLEncoder.encode(urn);
		} else {
			url = "../servlets/NBNRegistration/Document-" + URLEncoder.encode(objectID) + ".start?urn=" + URLEncoder.encode(urn);
		}
			
		return url;
	}

}
