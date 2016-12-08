package org.mycore.webtools;

import org.mycore.webtools.processing.socket.MCRProcessableWebsocketSender;
import org.mycore.webtools.processing.socket.impl.MCRProcessableWebsocketSenderImpl;

import com.google.inject.AbstractModule;

public class MCRWebToolsModule extends AbstractModule {

    @Override
    protected void configure() {

        // PROCESSING GUI
        bind(MCRProcessableWebsocketSender.class).to(MCRProcessableWebsocketSenderImpl.class);

    }

}
