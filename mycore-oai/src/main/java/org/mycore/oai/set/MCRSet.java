package org.mycore.oai.set;

import org.mycore.oai.pmh.Set;

public class MCRSet extends Set {
    private String setId;

    public MCRSet(String setId, String spec, String name) {
        super(spec, name);
        this.setId = setId;
    }

    public MCRSet(String setId, String spec) {
        this(setId, spec, null);
    }

    public String getSetId() {
        return setId;
    }
}
