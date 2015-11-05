package org.mycore.sword.application;

import org.swordapp.server.AuthCredentials;
import org.swordapp.server.SwordAuthException;

/**
 * Authenticates a User with AuthCredentials.
 * @author Sebastian Hofmann (mcrshofm)
 */
public abstract class MCRSwordAuthHandler {
    public abstract void authentication(AuthCredentials credentials) throws SwordAuthException;
}
