/**
 * 
 */
package org.mycore.backend.jpa;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Converts {@link MCRObjectID} to {@link String}.
 * @author Thomas Scheffler
 *
 */
@Converter
public class MCRObjectIDConverter implements AttributeConverter<MCRObjectID, String> {

    @Override
    public String convertToDatabaseColumn(MCRObjectID id) {
        return id.toString();
    }

    @Override
    public MCRObjectID convertToEntityAttribute(String id) {
        return id == null ? null : MCRObjectID.getInstance(id);
    }

}
