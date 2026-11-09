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

package org.mycore.pi.urn;

import static org.mycore.pi.urn.MCRDNBURN.URN_NID;
import static org.mycore.pi.urn.MCRUniformResourceName.PREFIX;

import java.util.Optional;

import org.mycore.pi.MCRPIParser;

/**
 * A {@link MCRDNBURNParser} is a {@link MCRPIParser} for {@link MCRDNBURN} identifier,
 * i.e. URNs in the <code>urn:nbn:de</code> namespace, which ara managed by the DNB.
 * <p>
 * While the DNB does no longer incudes checksums to URNs they create, MyCoRe
 * still supports adding checksums, using the algorithm formally used by the DNB.
 * <p>
 * If a parsed values ends with {@link MCRDNBURNParser#APPEND_CHECKSUM_MARKER},
 * that marker is replaced by a calculated checksum; otherwise parsed values are not modified.
 * This allows importing of URNs with and without checksums without modifications
 * (when no marker is added), and with automatic addition of the checksum.
 */
public class MCRDNBURNParser implements MCRPIParser<MCRDNBURN> {

    public static final String APPEND_CHECKSUM_MARKER = "?";

    @Override
    public Optional<MCRDNBURN> parse(String identifier) {
        String prefix = PREFIX + URN_NID;
        if (identifier.startsWith(prefix)) {
            int lastColon = identifier.lastIndexOf(':') + 1;

            String namespace = identifier.substring(prefix.length(), lastColon);
            String nsss = identifier.substring(lastColon);

            if (nsss.endsWith(APPEND_CHECKSUM_MARKER)) {
                return Optional.of(new MCRDNBURN(namespace, nsss.substring(0, nsss.length() - 1), true));
            } else {
                return Optional.of(new MCRDNBURN(namespace, nsss, false));
            }

        }

        return Optional.empty();
    }

}
