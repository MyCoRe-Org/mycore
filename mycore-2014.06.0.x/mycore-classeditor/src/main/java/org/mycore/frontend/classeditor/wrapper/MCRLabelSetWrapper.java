package org.mycore.frontend.classeditor.wrapper;

import java.util.Set;

import org.mycore.datamodel.classifications2.MCRLabel;

public class MCRLabelSetWrapper {

    private Set<MCRLabel> labelSet;

    public MCRLabelSetWrapper(Set<MCRLabel> labels) {
        this.labelSet = labels;
    }

    public Set<MCRLabel> getSet() {
        return labelSet;
    }

}
