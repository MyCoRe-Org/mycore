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

package org.mycore.pi;

import java.util.function.Function;
import java.util.stream.Stream;

import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;

public class MCRLocalPIResolver extends MCRPIResolver<MCRPersistentIdentifier> {
    private final Function<String, String> toReceiveObjectURL = mcrID -> MCRFrontendUtil.getBaseURL() + "receive/"
        + mcrID;

    public MCRLocalPIResolver() {
        super("Local-Resolver");
    }

    @Override
    public Stream<String> resolve(MCRPersistentIdentifier identifier) throws MCRIdentifierUnresolvableException {
        return MCRPIManager.getInstance()
            .getInfo(identifier)
            .stream()
            .map(MCRPIRegistrationInfo::getMycoreID)
            .map(toReceiveObjectURL);
    }
}
