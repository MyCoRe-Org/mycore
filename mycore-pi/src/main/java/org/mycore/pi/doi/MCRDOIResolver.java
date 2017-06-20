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
