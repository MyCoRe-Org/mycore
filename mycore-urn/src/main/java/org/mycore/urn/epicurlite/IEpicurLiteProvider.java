/**
 * 
 */
package org.mycore.urn.epicurlite;

import java.net.URL;

import org.mycore.urn.hibernate.MCRURN;


/**
 * @author shermann
 *
 */
public interface IEpicurLiteProvider {

    /**
     * Creates the {@link EpicurLite} for the given {@link MCRURN}.
     * 
     * @param urn
     * @return
     */
    public EpicurLite getEpicurLite(MCRURN urn);

    /**
     * @param urn
     * @return
     */
    public URL getURL(MCRURN urn);
}
