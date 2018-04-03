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

import java.util.Optional;

import org.mycore.pi.MCRPIParser;

import static org.mycore.pi.urn.MCRDNBURN.URN_NID;
import static org.mycore.pi.urn.MCRUniformResourceName.PREFIX;

public class MCRDNBURNParser implements MCRPIParser<MCRDNBURN> {

    @Override
    public Optional<MCRDNBURN> parse(String identifier) {
        String prefix = PREFIX + URN_NID;
        if (identifier.startsWith(prefix)) {
            int lastColon = identifier.lastIndexOf(":") + 1;
            int checkSumStart = identifier.length() - 1;

            String namespace = identifier.substring(prefix.length(), lastColon);
            String nsss = identifier.substring(lastColon, checkSumStart);

            return Optional.of(new MCRDNBURN(namespace, nsss));
        }

        return Optional.empty();
    }
}
