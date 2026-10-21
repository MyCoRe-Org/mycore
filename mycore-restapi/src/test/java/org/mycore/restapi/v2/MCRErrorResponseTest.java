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

package org.mycore.restapi.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

class MCRErrorResponseTest {

    @Test
    void testToExceptionWithStatusCodeOfEnum() {
        WebApplicationException exception = MCRErrorResponse.ofStatusCode(404).toException();
        assertEquals(404, exception.getResponse().getStatus());
        assertEquals(NotFoundException.class, exception.getClass());
    }

    @Test
    void testToExceptionWithStatusCodeNotInEnum() {
        WebApplicationException exception = MCRErrorResponse.ofStatusCode(422).toException();
        assertEquals(422, exception.getResponse().getStatus());
        assertEquals(ClientErrorException.class, exception.getClass());
    }
}
