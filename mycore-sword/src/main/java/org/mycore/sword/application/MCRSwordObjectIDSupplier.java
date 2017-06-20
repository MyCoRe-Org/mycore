package org.mycore.sword.application;

import java.util.List;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.swordapp.server.SwordServerException;

/**
 * Should supply the mycore sword implementation with the right MyCoReIds. It also handles pagination.
 * The Standard impl. is {@link MCRSwordSolrObjectIDSupplier} and it should handle 99% of all use cases.
 */
public abstract class MCRSwordObjectIDSupplier {
    /**
     * @return how many objects a collection has
     * @throws SwordServerException if an error occurs while determining the result
     */
    public abstract long getCount() throws SwordServerException;

    /**
     * @param from first object id which should appear in the list
     * @param count count how many ids should appear in the list
     * @return a list of MyCoReObjectIDs
     * @throws SwordServerException if an error occurs while determining the result
     */
    public abstract List<MCRObjectID> get(int from, int count) throws SwordServerException;
}
