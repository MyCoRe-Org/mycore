package org.mycore.datamodel.classifications2.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * Used for specific JPA queries only. Do not use directly!
 * @author Thomas Scheffler (yagee)
 * @since 2016.04
 */
public class MCRCategoryDTO {
    public static final String SELECT = "select new org.mycore.datamodel.classifications2.impl.MCRCategoryDTO(cat.internalID, cat.URI, cat.id, cat.left, cat.right, cat.level, labels.lang, labels.text, labels.description) from MCRCategoryImpl cat LEFT OUTER JOIN cat.labels labels";

    public static final String CAT_SELECT = "select new org.mycore.datamodel.classifications2.impl.MCRCategoryDTO(cat.internalID, cat.URI, cat.id, cat.left, cat.right, cat.level) from MCRCategoryImpl cat";

    public static final String LRL_SELECT = "select new org.mycore.datamodel.classifications2.impl.MCRCategoryDTO(cat.left, cat.right, cat.level) from MCRCategoryImpl cat";

    int internalID;

    URI uri;

    MCRCategoryID id;

    int leftValue, level, rightValue;

    String lang, text, description;

    public MCRCategoryDTO(int internalID, URI uri, MCRCategoryID id, int leftValue, int rightValue,
        int level, String lang, String text, String description) {
        this(internalID, uri, id, leftValue, rightValue, level);
        this.lang = lang;
        this.text = text;
        this.description = description;
    }

    public MCRCategoryDTO(int internalID, URI uri, MCRCategoryID id, int leftValue, int rightValue,
        int level) {
        this(leftValue, rightValue, level);
        this.internalID = internalID;
        this.uri = uri;
        this.id = id;
    }

    public MCRCategoryDTO(int leftValue, int rightValue, int level) {
        this.leftValue = leftValue;
        this.rightValue = rightValue;
        this.level = level;
    }

    public MCRCategoryImpl merge(MCRCategoryImpl predecessor) {
        if (predecessor == null) {
            return toCategory();
        }
        if (predecessor.getInternalID() == internalID) {
            //only add label
            return appendLabel(predecessor);
        }
        MCRCategoryImpl cat = toCategory();
        MCRCategory parent = predecessor;
        while (parent.getLevel() >= level) {
            parent = parent.getParent();
        }
        parent.getChildren().add(cat);
        cat.setLevel(level); //is reset to parent.level+1 in step before
        return cat;
    }

    private MCRCategoryImpl toCategory() {
        MCRCategoryImpl cat = new MCRCategoryImpl();
        cat.setInternalID(internalID);
        cat.setURI(uri);
        cat.setId(id);
        if (cat.getId().isRootID()) {
            cat.setRoot(cat);
        }
        cat.setLeft(leftValue);
        cat.setRight(rightValue);
        cat.setLevel(level);
        cat.setChildren(new ArrayList<>());
        return appendLabel(cat);
    }

    private MCRCategoryImpl appendLabel(MCRCategoryImpl cat) {
        if (lang != null) {
            MCRLabel label = new MCRLabel(lang, text,
                Optional.ofNullable(description).filter(s -> !s.isEmpty()).orElse(null));
            cat.getLabels().add(label);
        }
        return cat;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT,
            "MCRCategoryDTO [internalID=%s, id=%s, uri=%s, leftValue=%s, level=%s, rightValue=%s, lang=%s, text=%s, description=%s]",
            internalID, id, uri, leftValue, level, rightValue, lang, text, description);
    }

    public int getInternalID() {
        return internalID;
    }

}
