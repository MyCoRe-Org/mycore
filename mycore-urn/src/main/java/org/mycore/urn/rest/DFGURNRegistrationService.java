/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.urn.rest;

import java.util.List;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.urn.epicurlite.DFGViewerEpicurLiteProvider;
import org.mycore.urn.epicurlite.IEpicurLiteProvider;
import org.mycore.urn.hibernate.MCRURN;
import org.mycore.urn.services.MCRURNManager;

/**
 * @author shermann
 *
 */
@Deprecated
public class DFGURNRegistrationService extends URNRegistrationService {

    @SuppressWarnings("unchecked")
    public DFGURNRegistrationService() throws Exception {
        super();
        // use different epicur lite provider
        String clazz = MCRConfiguration.instance().getString("MCR.URN.EpicurLiteProvider.DFG.Class",
            DFGViewerEpicurLiteProvider.class.getName());
        Class<IEpicurLiteProvider> c = (Class<IEpicurLiteProvider>) Class.forName(clazz);
        epicurLiteProvider = c.newInstance();
    }

    /* (non-Javadoc)
     * @see org.mycore.urn.rest.URNRegistrationService#getURN()
     */
    @Override
    List<MCRURN> getURNList() {
        LOGGER.info("Getting URN for DFG Viewer link");
        List<MCRURN> list = MCRURNManager.getBaseURN(true, false, 0, 256);

        for (MCRURN urn : list) {
            // change niss
            String nissChanged = urn.toString().replace(urnProvider.getNISS(), urnProvider.getNISS() + "-dfg");
            org.mycore.urn.services.MCRURN dfg = org.mycore.urn.services.MCRURN.create(nissChanged);

            // modify old urn for posting it to the epicur lite provider
            urn.getKey().setMcrurn(dfg.toString());
        }

        return list;
    }
}
