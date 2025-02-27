/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
    @Override
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
