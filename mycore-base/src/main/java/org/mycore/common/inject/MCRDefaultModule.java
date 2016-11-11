package org.mycore.common.inject;

import org.mycore.common.processing.MCRProcessableRegistry;
import org.mycore.common.processing.impl.MCRCentralProcessableRegistry;

import com.google.inject.AbstractModule;

/**
 * Default module to bind mycore guice dependencies.
 * 
 * @author Matthias Eichner
 */
public class MCRDefaultModule extends AbstractModule {

    @Override
    protected void configure() {

        // PROCESSING API
        bind(MCRProcessableRegistry.class).to(MCRCentralProcessableRegistry.class);

    }

}
