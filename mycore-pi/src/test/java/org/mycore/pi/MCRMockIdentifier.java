package org.mycore.pi;

public class MCRMockIdentifier implements MCRPersistentIdentifier {
    private String text;

    protected MCRMockIdentifier(String text) {
        this.text = text;
    }

    @Override
    public String asString() {
        return this.text;
    }

    public static final String MOCK_SCHEME = "MOCK:";

}
