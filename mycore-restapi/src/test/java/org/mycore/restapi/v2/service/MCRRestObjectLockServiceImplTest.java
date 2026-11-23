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

package org.mycore.restapi.v2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.frontend.support.MCRObjectLock;
import org.mycore.test.MyCoReTest;

import jakarta.ws.rs.BadRequestException;

@MyCoReTest
@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "MCR.RestAPI.V2.ObjectLock.Timeout.Max", string = "1000")
    })
public class MCRRestObjectLockServiceImplTest {

    @Test
    void requestedTimeoutIsOptional() {
        assertNull(MCRRestObjectLockServiceImpl.getRequestedTimeout(null));
        assertNull(MCRRestObjectLockServiceImpl.getRequestedTimeout(new MCRObjectLock()));
    }

    @Test
    void requestedTimeoutIsLimited() {
        assertEquals(500, MCRRestObjectLockServiceImpl.getRequestedTimeout(new MCRObjectLock().setTimeout(500)));
        assertEquals(1000,
            MCRRestObjectLockServiceImpl.getRequestedTimeout(new MCRObjectLock().setTimeout(Integer.MAX_VALUE)));
    }

    @Test
    void requestedTimeoutMustBePositive() {
        assertThrows(BadRequestException.class,
            () -> MCRRestObjectLockServiceImpl.getRequestedTimeout(new MCRObjectLock().setTimeout(0)));
        assertThrows(BadRequestException.class,
            () -> MCRRestObjectLockServiceImpl.getRequestedTimeout(new MCRObjectLock().setTimeout(-1)));
    }

}
