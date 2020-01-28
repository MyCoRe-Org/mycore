package org.mycore.services.staticcontent;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;

public class MCRStaticContentEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        handleObjectUpdated(evt, obj);
    }

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        handleObjectUpdated(evt, obj);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        MCRObjectStaticContentGenerator.getContentGenerators()
            .stream()
            .map(MCRObjectStaticContentGenerator::new)
            .forEach(cg -> {
                try {
                    cg.generate(obj);
                } catch (IOException e) {
                    LOGGER.error(
                        "Error while creating static content " + cg.getTransformer() + " for " + obj.getId() + "!", e);
                }
            });
    }
}
