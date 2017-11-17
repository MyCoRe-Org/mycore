package org.mycore.viewer.alto.guice;

import org.mycore.viewer.alto.service.MCRAltoChangeApplier;
import org.mycore.viewer.alto.service.MCRAltoChangeSetStore;
import org.mycore.viewer.alto.service.MCRDerivateTitleResolver;
import org.mycore.viewer.alto.service.impl.MCRDefaultAltoChangeApplier;
import org.mycore.viewer.alto.service.impl.MCRDefaultDerivateTitleResolver;
import org.mycore.viewer.alto.service.impl.MCRJPAAltoChangeSetStore;

import com.google.inject.AbstractModule;

public class MCRViewerALTOTestModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(MCRAltoChangeSetStore.class).to(MCRJPAAltoChangeSetStore.class);
        bind(MCRDerivateTitleResolver.class).to(MCRDefaultDerivateTitleResolver.class);
        bind(MCRAltoChangeApplier.class).to(MCRDefaultAltoChangeApplier.class);
    }
}
