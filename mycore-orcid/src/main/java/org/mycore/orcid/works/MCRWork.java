/*
* This file is part of *** M y C o R e ***
* See http://www.mycore.de/ for details.
*
* This program is free software; you can use it, redistribute it
* and / or modify it under the terms of the GNU General Public License
* (GPL) as published by the Free Software Foundation; either version 2
* of the License or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but
* WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program, in a file called gpl.txt or license.txt.
* If not, write to the Free Software Foundation Inc.,
* 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
*/

package org.mycore.orcid.works;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStringContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.mods.bibtex.MCRBibTeX2MODSTransformer;
import org.mycore.mods.merger.MCRMergeTool;
import org.mycore.orcid.MCRORCIDNamespaces;

/**
 * Represents a single "work", that means a publication within the "works" section of an ORCID profile.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRWork {

    private final static Logger LOGGER = LogManager.getLogger(MCRWork.class);

    /** Transformer used to parse bibTeX to MODS */
    private final static MCRContentTransformer T_BIBTEX2MODS = new MCRBibTeX2MODSTransformer();

    private String putCode;

    private Element mods;

    /**
     * Returns the put code, which is the unique identifier of this work within the ORCID profile
     *
     * @return the put code of the work
     */
    public String getPutCode() {
        return putCode;
    }

    /**
     * Returns the MODS representation of the work's publication data
     */
    public Element getMODS() {
        return mods;
    }

    void setFromWorkXML(Element workXML) {
        this.putCode = workXML.getAttributeValue("put-code");
        this.mods = workXML.getChild("mods", MCRConstants.MODS_NAMESPACE).detach();
        mergeMODSfromBibTeX(workXML);
    }

    /**
     * Parses the bibTeX that may be present at the XPath
     * work:citation[work:citation-type='bibtex']/work:citation-value/
     * and merges the resulting MODS into the MODS we already got from the rest of the XML
     *
     * @param workXML the work:work element from the ORCID response
     */
    private void mergeMODSfromBibTeX(Element workXML) {
        for (Element citation : workXML.getChildren("citation", MCRORCIDNamespaces.NS_WORK)) {
            String type = citation.getChildTextTrim("citation-type", MCRORCIDNamespaces.NS_WORK);
            if (!"bibtex".equals(type)) {
                continue;
            }

            String bibTeX = citation.getChildTextTrim("citation-value", MCRORCIDNamespaces.NS_WORK);
            if (bibTeX.isEmpty()) {
                continue;
            }

            try {
                MCRContent result = T_BIBTEX2MODS.transform(new MCRStringContent(bibTeX));
                Element modsCollection = result.asXML().getRootElement();
                Element modsFromBibTeX = modsCollection.getChild("mods", MCRConstants.MODS_NAMESPACE);
                // Remove mods:extension containing the original BibTeX:
                modsFromBibTeX.removeChildren("extension", MCRConstants.MODS_NAMESPACE);
                MCRMergeTool.merge(this.mods, modsFromBibTeX);
            } catch (Exception ex) {
                String msg = "Exception parsing BibTeX: " + bibTeX;
                LOGGER.warn(msg + " " + ex.getMessage());
            }
        }
    }
}
