/**
 *
 * $Revision: 16886 $ $Date: 2010-03-12 16:54:13 +0100 (Fr, 12 Mrz 2010) $
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.urn.services;

import java.util.UUID;

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
 */
@Deprecated
public class MCRNISSBuilderUUID implements MCRNISSBuilder {

    public void init(String configID) {
    }

    public String buildNISS() {
        UUID u = UUID.randomUUID();
        String niss = u.toString();

        return niss;
    }
}
