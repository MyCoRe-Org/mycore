package org.mycore.datamodel.metadata.history;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class MCRMetadataHistoryEventTypeConverter implements AttributeConverter<MCRMetadataHistoryEventType, String> {

    @Override
    public String convertToDatabaseColumn(MCRMetadataHistoryEventType event) {
        return String.valueOf(event.getAbbr());
    }

    @Override
    public MCRMetadataHistoryEventType convertToEntityAttribute(String c) {
        if (c.length() != 1) {
            throw new IllegalArgumentException("Accept only single character values: " + c);
        }
        return MCRMetadataHistoryEventType.fromAbbr(c.charAt(0));
    }

}
