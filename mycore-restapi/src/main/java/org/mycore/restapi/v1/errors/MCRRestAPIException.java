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

package org.mycore.restapi.v1.errors;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.Response.Status;

/**
 * exception that can be thrown during rest api requests
 * 
 * if no other status is suitable,  use 501 = Status.INTERNAL_SERVER_ERROR
 * 
 * @author Robert Stephan
 * 
 */
public class MCRRestAPIException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<MCRRestAPIError> errors = new ArrayList<>();

    private Status status;

    public MCRRestAPIException(Status status, MCRRestAPIError error) {
        this.status = status;
        errors.add(error);
    }

    public MCRRestAPIException(Status status, List<MCRRestAPIError> errors) {
        this.status = status;
        this.errors.addAll(errors);
    }

    public List<MCRRestAPIError> getErrors() {
        return errors;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
