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

package org.mycore.pi.doi;

import java.util.stream.Stream;

import org.mycore.pi.MCRPIResolver;
import org.mycore.pi.doi.client.datacite.MCRDataciteRest;
import org.mycore.pi.doi.client.datacite.MCRDataciteRestResponse;
import org.mycore.pi.doi.client.datacite.MCRDataciteRestResponseEntry;
import org.mycore.pi.doi.client.datacite.MCRDataciteRestResponseEntryDataStringValue;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;

public class MCRDOIResolver extends MCRPIResolver<MCRDigitalObjectIdentifier> {
    public MCRDOIResolver() {
        super("DOI-Resolver");
    }

    @Override
    public Stream<String> resolve(MCRDigitalObjectIdentifier identifier) throws MCRIdentifierUnresolvableException {
        MCRDataciteRestResponse restResponse = MCRDataciteRest.get(identifier);

        if (restResponse.getResponseCode() == 1) {
            return restResponse.getValues()
                .stream()
                .filter(responseEntry -> responseEntry.getType().equals("URL"))
                .map(MCRDataciteRestResponseEntry::getData)
                .filter(responseEntryData -> responseEntryData.getFormat().equals("string"))
                .map(responseEntryData -> ((MCRDataciteRestResponseEntryDataStringValue) responseEntryData
                    .getValue()).getValue());
        }

        return Stream.empty();
    }
}
