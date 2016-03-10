/**
 * 
 */
package org.mycore.backend.jpa;

import java.net.URI;
import java.util.Optional;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Converts a {@link URI} to {@link String} for JPA 2.1.
 * @author Thomas Scheffler (yagee)
 */
@Converter
public class MCRURIConverter implements AttributeConverter<URI, String> {

    @Override
    public String convertToDatabaseColumn(URI attribute) {
        return Optional.ofNullable(attribute)
            .map(URI::toString)
            .orElse(null);
    }

    @Override
    public URI convertToEntityAttribute(String dbData) {
        return Optional.ofNullable(dbData)
            .map(URI::create)
            .orElse(null);
    }

}
