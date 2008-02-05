/*
 * $RCSfile$
 * $Revision$ $Date$
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

package org.mycore.services.webservices;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.jdom.Document;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.common.MCRXMLTableManager;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRResults;

/**
 * This class contains MyCoRe Webservices
 * 
 * @author Harald Richter
 * 
 * @version $Revision$ $Date$
 * 
 */
public class MCRWebService implements MCRWS {
    private static final Logger logger = Logger.getLogger(MCRWebService.class);

    private static MCRXMLTableManager TM = MCRXMLTableManager.instance();

    /*
     * (non-Javadoc)
     * 
     * @see MCRWS#MCRDoRetrieveObject(java.lang.String)
     */
    public org.w3c.dom.Document MCRDoRetrieveObject(String ID) throws Exception {
        // check the ID and retrieve the data
        org.jdom.Document d = TM.readDocument(new MCRObjectID(ID));

        org.jdom.output.DOMOutputter doo = new org.jdom.output.DOMOutputter();

        if (logger.isDebugEnabled()) {
            org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
            logger.debug(outputter.outputString(d));
        }

        return doo.output(d);
    }

    /*
     * (non-Javadoc)
     * 
     * @see MCRWS#MCRDoRetrieveClassification(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public org.w3c.dom.Document MCRDoRetrieveClassification(String level, String type, String classID, String categID, String format ) throws Exception {
        if ( null == format )
          format = "metadata";

        String uri = "classification:" + format + ":" + level + ":" + type + ":" + classID + ":" + categID;
        org.jdom.Element cl = MCRURIResolver.instance().resolve(uri);

        if (logger.isDebugEnabled()) {
            org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
            logger.debug(outputter.outputString(cl));
        }

        org.jdom.Document d = new org.jdom.Document( (org.jdom.Element)( cl.clone() ) );
        return new org.jdom.output.DOMOutputter().output(d);
    }

    /*
     * (non-Javadoc)
     * 
     * @see MCRWS#MCRDoQuery(org.w3c.dom.Document)
     */
    public org.w3c.dom.Document MCRDoQuery(org.w3c.dom.Document query) throws Exception {
        org.jdom.input.DOMBuilder d = new org.jdom.input.DOMBuilder();
        Document doc = d.build(query);

        if (logger.isDebugEnabled()) {
            org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
            logger.debug(outputter.outputString(doc));
        }

        // Execute query
        MCRResults res = MCRQueryManager.search(MCRQuery.parseXML(doc), true);
        Document result = new Document(res.buildXML());

        org.jdom.output.DOMOutputter doo = new org.jdom.output.DOMOutputter();
        return doo.output(result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see MCRWS#MCRDoQuery(org.w3c.dom.Document)
     */
    public org.w3c.dom.Document MCRDoRetrieveLinks(String from, String to, String type) throws Exception {
        // set default empty answer
        MCRResults results = new MCRResults();
        // check parameter
        if ((type == null) || (type.length() == 0)) {
            type = MCRLinkTableManager.ENTRY_TYPE_REFERENCE;
        }
        if (from == null)
            from = "";
        if (to == null)
            to = "";
        if ((from.length() != 0) || (to.length() != 0)) {
          logger.debug("Input parameter : type=" + type + "   from=" + from + "   to=" + to);
          List links = new ArrayList();
          MCRLinkTableManager LM = MCRLinkTableManager.instance();
          // Look for links
          if ((from = from.trim()).length() != 0) {
            // logger.debug("Use MCRLinkTableManager.getDestinationOf("+from+","+type+")");
            links = LM.getDestinationOf(from, type);
        } else {
            // logger.debug("Use MCRLinkTableManager.getSourceOf("+to+","+type+")");
            links = LM.getSourceOf(to, type);
        }
        // logger.debug("Get "+(new Integer(links.size())).toString()+" results");
        for (int i = 0; i < links.size(); i++) {
            MCRHit hit = new MCRHit((String) links.get(i));
            results.addHit(hit);
        }
        } else {
            logger.warn("Input parameter from and to are empty!");
        }
        // write output
        Document result = new Document(results.buildXML());
        org.jdom.output.DOMOutputter doo = new org.jdom.output.DOMOutputter();
        return doo.output(result);
    }
    
}
