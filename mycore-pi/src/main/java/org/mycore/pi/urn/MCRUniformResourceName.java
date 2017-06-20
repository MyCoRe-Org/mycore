package org.mycore.pi.urn;

import org.mycore.pi.MCRPersistentIdentifier;

public class MCRUniformResourceName implements MCRPersistentIdentifier {
    public static final String PREFIX = "urn:";

    public String getPREFIX() {
        return PREFIX;
    }

    protected MCRUniformResourceName() {
    };

    public MCRUniformResourceName(String subNamespace, String namespaceSpecificString) {
        this.subNamespace = subNamespace;
        this.namespaceSpecificString = namespaceSpecificString;
    }

    protected String subNamespace;

    protected String namespaceSpecificString;

    public String getSubNamespace() {
        return subNamespace;
    }

    public String getNamespaceSpecificString() {
        return namespaceSpecificString;
    }

    @Override
    public String asString() {
        return getPREFIX() + getSubNamespace() + getNamespaceSpecificString();
    }
}
