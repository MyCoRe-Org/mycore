package org.mycore.datamodel.common;

/**
 * This enum represents the different types of links in the MyCoRe system.
 */
public enum MCRLinkType {
    CHILD("child"),
    DERIVATE("derivate"),
    DERIVATE_LINK("derivate_link"),
    PARENT("parent"),
    REFERENCE("reference");

    private final String relationName;

    MCRLinkType(String relationName) {
        this.relationName = relationName;
    }

    @Override
    public String toString() {
        return relationName;
    }

    public boolean isDerivateLinkType() {
        return this == DERIVATE || this == DERIVATE_LINK;
    }

    public static MCRLinkType fromString(String text) {
        for (MCRLinkType b : values()) {
            if (b.relationName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }

    public MCRLinkType getOppositeType() {
        return switch (this) {
            case CHILD -> PARENT;
            case PARENT -> CHILD;
            case DERIVATE -> DERIVATE;
            case DERIVATE_LINK -> DERIVATE_LINK;
            case REFERENCE -> REFERENCE;
        };
    }
}
