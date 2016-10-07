package org.mycore.iiif.presentation.model.attributes;

import java.util.Optional;

public class MCRIIIFMetadata {

    private String label;

    private Object value;

    public MCRIIIFMetadata(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public MCRIIIFMetadata(String label, MCRIIIFMetadataValue value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Optional<MCRIIIFMetadataValue> getValue() {
        if (!(this.value instanceof MCRIIIFMetadataValue)) {
            return Optional.empty();
        }
        return Optional.of((MCRIIIFMetadataValue) this.value);
    }

    public Optional<String> getStringValue() {
        if (!(this.value instanceof String)) {
            return Optional.empty();
        }
        return Optional.of((String) this.value);
    }

}
