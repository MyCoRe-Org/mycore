package org.mycore.datamodel.classifications2.utils;

import java.util.Collection;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRLabel;

public class MCRStringTransformer {

    public static String getString(MCRCategory category) {
        StringBuilder sb = new StringBuilder(1024);
        printCatgory(category, sb);
        return sb.toString();
    }

    private static void printCatgory(MCRCategory category, StringBuilder sb) {
        for (int i = 0; i < category.getLevel(); i++) {
            sb.append(' ');
        }
        sb.append(category.getId());
        sb.append('[');
        printLabels(category.getLabels(), sb);
        sb.append(']');
        sb.append('\n');
        for (MCRCategory child : category.getChildren()) {
            printCatgory(child, sb);
        }
    }

    private static void printLabels(Collection<MCRLabel> labels, StringBuilder sb) {
        for (MCRLabel label : labels) {
            sb.append(label);
            sb.append(',');
        }
        if (labels.size() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
    }

}
