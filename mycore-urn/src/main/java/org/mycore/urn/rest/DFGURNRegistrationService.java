/**
 * 
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
