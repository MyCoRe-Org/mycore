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

package org.mycore.pi.doi.client.datacite;

import java.util.Base64;

public class MCRDataciteRestResponseEntryDataBase64Value extends MCRDataciteRestResponseEntryDataValue {

    private final byte[] decodedValue;

    public MCRDataciteRestResponseEntryDataBase64Value(String base64value) {
        decodedValue = Base64.getDecoder().decode(base64value);
    }

    public byte[] getDecodedValue() {
        return decodedValue.clone();
    }

}
