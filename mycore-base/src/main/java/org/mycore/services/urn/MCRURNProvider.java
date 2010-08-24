/**
 * 
 */
package org.mycore.services.urn;

import java.util.List;
import java.util.Vector;


/**
 * @author shermann
 * 
 */
public class MCRURNProvider extends AbstractURNProvider {

    /* (non-Javadoc)
     * @see fsu.archiv.mycore.urn.IURNProvider#generateURN()
     */
    public URN generateURN() {
        String urn = MCRURNManager.buildURN("UBL");
        return URN.valueOf(urn);
    }

    /* (non-Javadoc)
     * @see fsu.archiv.mycore.urn.IURNProvider#generateURN(int)
     */
    public URN[] generateURN(int amount) {
        List<URN> list = new Vector<URN>(amount);
        while (amount != 0) {
            list.add(generateURN());
            amount--;
        }
        return list.toArray(new URN[0]);
    }
}
