/*
 * $Id$
 * $Revision: 5697 $ $Date: Oct 25, 2013 $
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

package org.mycore.datamodel.metadata.share;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This factory creates {@link MCRMetadataShareAgent} instances.
 * 
 * To configure a custom metadata share agent define a property <code>MCR.Metadata.ShareAgent.{objectType}</code>
 * that points to a class that implements the {@link MCRMetadataShareAgent} interface.
 * @author Thomas Scheffler (yagee)
 * 
 */
public class MCRMetadataShareAgentFactory {

    private final static String CONFIG_PREFIX = "MCR.Metadata.ShareAgent.";

    private static final MCRDefaultMetadataShareAgent DEFAULT_AGENT = new MCRDefaultMetadataShareAgent();

    public static MCRMetadataShareAgent getAgent(MCRObjectID objectId) {
        String propertyName = CONFIG_PREFIX + objectId.getTypeId();
        String propertyValue = MCRConfiguration.instance().getString(propertyName, null);
        if (propertyValue != null) {
            //we will not get undefined problems here
            MCRMetadataShareAgent metadataShareAgent = MCRConfiguration.instance()
                .<MCRMetadataShareAgent> getSingleInstanceOf(propertyName, (String) null);
            return metadataShareAgent;
        }
        return getDefaultAgent();

    }

    /**
     * Standard agent that uses <code>org.mycore.datamodel.MCRMeta*</code> classes.
     */
    public static MCRMetadataShareAgent getDefaultAgent() {
        return DEFAULT_AGENT;
    }

}
