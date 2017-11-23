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

package org.mycore.pi.doi;

import java.util.stream.Stream;

import org.mycore.pi.MCRPersistentIdentifierResolver;
import org.mycore.pi.doi.rest.MCRDOIRest;
import org.mycore.pi.doi.rest.MCRDOIRestResponse;
import org.mycore.pi.doi.rest.MCRDOIRestResponseEntry;
import org.mycore.pi.doi.rest.MCRDOIRestResponseEntryDataStringValue;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;

public class MCRDOIResolver extends MCRPersistentIdentifierResolver<MCRDigitalObjectIdentifier> {
    public MCRDOIResolver() {
        super("DOI-Resolver");
    }

    @Override
    public Stream<String> resolve(MCRDigitalObjectIdentifier identifier) throws MCRIdentifierUnresolvableException {
        MCRDOIRestResponse restResponse = MCRDOIRest.get(identifier);

        if (restResponse.getResponseCode() == 1) {
            return restResponse.getValues()
                .stream()
                .filter(responseEntry -> responseEntry.getType().equals("URL"))
                .map(MCRDOIRestResponseEntry::getData)
                .filter(responseEntryData -> responseEntryData.getFormat().equals("string"))
                .map(responseEntryData -> ((MCRDOIRestResponseEntryDataStringValue) responseEntryData
                    .getValue()).getValue());
        }

        return Stream.empty();
    }
}
