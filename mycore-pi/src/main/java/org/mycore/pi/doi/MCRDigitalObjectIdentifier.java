package org.mycore.pi.doi;

import java.util.Locale;

import org.mycore.pi.MCRPersistentIdentifier;

public class MCRDigitalObjectIdentifier implements MCRPersistentIdentifier {

    public static final String TYPE = "doi";

    public static final String TEST_DOI_PREFIX = "10.5072";

    private String prefix;

    private String suffix;

    protected MCRDigitalObjectIdentifier(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public MCRDigitalObjectIdentifier toTestPrefix() {
        return new MCRDigitalObjectIdentifier(TEST_DOI_PREFIX, suffix);
    }

    @Override
    public String asString() {
        return String.format(Locale.ENGLISH, "%s/%s", prefix, suffix);
    }
}
