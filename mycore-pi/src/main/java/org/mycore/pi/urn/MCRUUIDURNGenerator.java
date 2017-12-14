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

package org.mycore.pi.urn;

import java.util.UUID;

import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Builds a new, unique NISS using Java implementation of the UUID
 * specification. java.util.UUID creates 'only' version 4 UUIDs.
 * Version 4 UUIDs are generated from a large random number and do
 * not include the MAC address.
 *
 * UUID = 8*HEX "-" 4*HEX "-" 4*HEX "-" 4*HEX "-" 12*HEX
 * Example One: 067e6162-3b6f-4ae2-a171-2470b63dff00
 * Example Two: 54947df8-0e9e-4471-a2f9-9af509fb5889
 *
 * @author Kathleen Neumann (kkrebs)
 * @author Sebastian Hofmann
 */
public class MCRUUIDURNGenerator extends MCRDNBURNGenerator {

    public MCRUUIDURNGenerator(String generatorID) {
        super(generatorID);
    }

    @Override
    protected String buildNISS(MCRObjectID mcrID, String additional) {
        return UUID.randomUUID().toString();
    }
}
