package org.mycore.sword.application;

import static org.mycore.user2.MCRUserManager.exists;
import static org.mycore.user2.MCRUserManager.login;

import org.mycore.user2.MCRUser;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.SwordAuthException;

/**
 * This implementation ignores the on-behalf-of header in request and just authenticate with MyCoRe user and password.
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSwordDefaultAuthHandler extends MCRSwordAuthHandler {
    public void authentication(AuthCredentials credentials) throws SwordAuthException {
        if (!exists(credentials.getUsername())) {
            throw new SwordAuthException("Wrong login data!");
        }
        MCRUser mcrUser = login(credentials.getUsername(), credentials.getPassword());
        if (mcrUser == null) {
            throw new SwordAuthException("Wrong login data!");
        }
    }
}
