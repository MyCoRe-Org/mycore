package org.mycore.pi.doi.rest;

import java.util.Base64;

public class MCRDOIRestResponseEntryDataBase64Value extends MCRDOIRestResponseEntryDataValue {

    private final byte[] decodedValue;

    public MCRDOIRestResponseEntryDataBase64Value(String base64value) {
        decodedValue = Base64.getDecoder().decode(base64value);
    }

    public byte[] getDecodedValue() {
        return decodedValue;
    }
}
