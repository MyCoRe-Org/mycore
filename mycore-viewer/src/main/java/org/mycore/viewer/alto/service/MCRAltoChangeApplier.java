package org.mycore.viewer.alto.service;

import org.mycore.viewer.alto.model.MCRAltoChangeSet;

public interface MCRAltoChangeApplier {
    void applyChange(MCRAltoChangeSet changeSet);
}
