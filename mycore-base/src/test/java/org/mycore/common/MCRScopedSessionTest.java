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

package org.mycore.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MCRScopedSessionTest extends MCRTestCase {

    /**
     * Verifies that MCR-3704 does not reappear when scoped values are rendered as text.
     */
    @Test
    public void doAsDoesNotCreateRecursiveScopedValuesStringRepresentation() {
        MCRScopedSession session = new MCRScopedSession();
        MCRScopedSession.ScopedValues values = new MCRScopedSession.ScopedValues(MCRSystemUserInformation.JANITOR);
        session.doAs(values, () -> {
            assertEquals(Boolean.TRUE, session.get(MCRScopedSession.SCOPED_HINT));
            values.toString();
            session.getMapEntries().toString();
        });
    }

}
