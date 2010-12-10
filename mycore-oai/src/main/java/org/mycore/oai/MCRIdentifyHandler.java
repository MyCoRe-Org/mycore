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

package org.mycore.oai;

import static org.mycore.oai.MCROAIConstants.NS_FRIENDS;
import static org.mycore.oai.MCROAIConstants.NS_OAI;
import static org.mycore.oai.MCROAIConstants.NS_OAI_ID;
import static org.mycore.oai.MCROAIConstants.NS_XSI;
import static org.mycore.oai.MCROAIConstants.SCHEMA_LOC_FRIENDS;
import static org.mycore.oai.MCROAIConstants.SCHEMA_LOC_OAI_ID;

import java.util.Iterator;
import java.util.Properties;

import org.jdom.Element;
import org.mycore.common.MCRConfiguration;

/**
 * Implements the Identify request.
 * 
 * @author Frank L\u00fctzenkirchen
 */
class MCRIdentifyHandler extends MCRVerbHandler {
    final static String VERB = "Identify";

    MCRIdentifyHandler(MCROAIDataProvider provider) {
        super(provider);
    }

    void handleRequest() {
        output.addContent(new Element("repositoryName", NS_OAI).setText(provider.getRepositoryName()));
        output.addContent(new Element("baseURL", NS_OAI).setText(provider.getOAIBaseURL()));
        output.addContent(new Element("protocolVersion", NS_OAI).setText("2.0"));
        output.addContent(new Element("earliestDatestamp", NS_OAI).setText(provider.getAdapter().getEarliestDatestamp()));
        output.addContent(new Element("deletedRecord", NS_OAI).setText(provider.getDeletedRecordPolicy()));
        output.addContent(new Element("granularity", NS_OAI).setText(MCROAIConstants.GRANULARITY));
        output.addContent(new Element("adminEmail", NS_OAI).setText(provider.getAdminEmail()));

        // Add OAI Identifier description
        Element description = new Element("description", NS_OAI);
        output.addContent(description);

        Element oaiIdentifier = new Element("oai-identifier", NS_OAI_ID);
        oaiIdentifier.setAttribute("schemaLocation", SCHEMA_LOC_OAI_ID, NS_XSI);
        oaiIdentifier.addNamespaceDeclaration(NS_XSI);
        description.addContent(oaiIdentifier);

        oaiIdentifier.addContent(new Element("scheme", NS_OAI_ID).setText("oai"));
        oaiIdentifier.addContent(new Element("repositoryIdentifier", NS_OAI_ID).setText(provider.getRepositoryIdentifier()));
        oaiIdentifier.addContent(new Element("delimiter", NS_OAI_ID).setText(":"));

        Element sampleIdentifier = new Element("sampleIdentifier", NS_OAI_ID);
        sampleIdentifier.setText("oai:" + provider.getRepositoryIdentifier() + ":" + provider.getRecordSampleID());
        oaiIdentifier.addContent(sampleIdentifier);

        addFriends();
    }

    /**
     * Adds a list of other OAI data providers that are friends
     */
    @SuppressWarnings("rawtypes")
    private void addFriends() {
        MCRConfiguration config = MCRConfiguration.instance();
        Properties friends = config.getProperties(provider.getPrefix() + "Friends.");
        if (friends.isEmpty())
            return;

        Element description = new Element("description", NS_OAI);
        output.addContent(description);

        Element eFriends = new Element("friends", NS_FRIENDS);
        eFriends.setAttribute("schemaLocation", SCHEMA_LOC_FRIENDS, NS_XSI);
        eFriends.addNamespaceDeclaration(NS_XSI);
        description.addContent(eFriends);

        for (Iterator it = friends.values().iterator(); it.hasNext();) {
            String friend = (String) (it.next());
            eFriends.addContent(new Element("baseURL", NS_FRIENDS).setText(friend));
        }
    }
}
