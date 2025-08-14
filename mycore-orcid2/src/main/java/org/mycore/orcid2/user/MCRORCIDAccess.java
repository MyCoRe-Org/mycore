package org.mycore.orcid2.user;

import org.mycore.access.MCRAccessException;
import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.user2.MCRUser;

import java.util.Set;

/**
 * Used to encapsulate access to a user's ORCIDs.
 */
public interface MCRORCIDAccess {

    void addORCID(String orcid, MCRUser user) throws MCRAccessException;

    Set<String> getORCIDs(MCRUser user);

    Set<MCRIdentifier> getIdentifiers(MCRUser user);
}
