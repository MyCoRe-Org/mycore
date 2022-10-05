/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

final class MCRErrorCodeConstants {

    public static final String API_NO_PERMISSION = "API_NO_PERMISSION";

    //Classification
    public static final String MCRCLASS_NOT_FOUND = "MCRCLASS_NOT_FOUND";

    public static final String MCRCLASS_ID_MISMATCH = "MCRCLASS_ID_MISMATCH";
    
    public static final String MCRCLASS_SERIALIZATION_ERROR = "MCRCLASS_SERIALIZATION_ERROR";


    //MCRObject
    public static final String MCROBJECT_NO_PERMISSION = "MCROBJECT_NO_PERMISSION";

    public static final String MCROBJECT_NOT_FOUND = "MCROBJECT_NOT_FOUND";

    public static final String MCROBJECT_REVISION_NOT_FOUND = "MCROBJECT_REVISION_NOT_FOUND";

    public static final String MCROBJECT_INVALID = "MCROBJECT_INVALID";

    public static final String MCROBJECT_ID_MISMATCH = "MCROBJECT_ID_MISMATCH";

    public static final String MCROBJECT_STILL_LINKED = "MCROBJECT_STILL_LINKED";

    public static final String MCROBJECT_INVALID_STATE = "MCROBJECT_INVALID_STATE";

    //MCRDerivate
    public static final String MCRDERIVATE_NO_PERMISSION = "MCRDERIVATE_NO_PERMISSION";

    public static final String MCRDERIVATE_NOT_FOUND = "MCRDERIVATE_NOT_FOUND";

    public static final String MCRDERIVATE_NOT_FOUND_IN_OBJECT = "MCRDERIVATE_NOT_FOUND_IN_OBJECT";

    public static final String MCRDERIVATE_INVALID = "MCRDERIVATE_INVALID";

    public static final String MCRDERIVATE_ID_MISMATCH = "MCRDERIVATE_ID_MISMATCH";

    //Derivate Content
    public static final String MCRDERIVATE_CREATE_DIRECTORY_ON_FILE = "MCRDERIVATE_CREATE_DIRECTORY_ON_FILE";

    public static final String MCRDERIVATE_CREATE_DIRECTORY = "MCRDERIVATE_CREATE_DIRECTORY";

    public static final String MCRDERIVATE_UPDATE_FILE = "MCRDERIVATE_UPDATE_FILE";

    public static final String MCRDERIVATE_FILE_NOT_FOUND = "MCRDERIVATE_FILE_NOT_FOUND";

    public static final String MCRDERIVATE_FILE_IO_ERROR = "MCRDERIVATE_FILE_IO_ERROR";

    public static final String MCRDERIVATE_NOT_DIRECTORY = "MCRDERIVATE_NOT_DIRECTORY";

    public static final String MCRDERIVATE_FILE_SIZE = "MCRDERIVATE_FILE_SIZE";

    public static final String MCRDERIVATE_DIRECTORY_NOT_EMPTY = "MCRDERIVATE_DIRECTORY_NOT_EMPTY";

    public static final String MCRDERIVATE_FILE_DELETE = "MCRDERIVATE_FILE_DELETE";

    public static final String MCRDERIVATE_NOT_FILE = "MCRDERIVATE_NOT_FILE";

    private MCRErrorCodeConstants() {
    }
}
