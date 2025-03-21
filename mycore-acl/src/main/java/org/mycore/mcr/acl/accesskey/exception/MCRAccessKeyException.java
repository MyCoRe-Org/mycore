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

package org.mycore.mcr.acl.accesskey.exception;

import org.mycore.common.MCRException;

import java.io.Serial;

/**
 * Instances of this class represent a general exception related to access keys.
 */
public class MCRAccessKeyException extends MCRException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Reference for error messages for i18n.
    */
    private String errorCode;

    public MCRAccessKeyException(String errorMessage) {
        super(errorMessage);
    }

    public MCRAccessKeyException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }

    public MCRAccessKeyException(String errorMessage, String errorCode) {
        super(errorMessage);
        this.errorCode = errorCode;
    }

    public MCRAccessKeyException(String errorMessage, String errorCode, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
