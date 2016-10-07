package org.mycore.iiif.presentation.model.attributes;

import com.google.gson.annotations.SerializedName;

public class MCRIIIFMetadataValue {

    @SerializedName("@value")
    private String value;

    @SerializedName("@language")
    private String language;

    public MCRIIIFMetadataValue(String value, String language) {
        this.value = value;
        this.language = language;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

}
