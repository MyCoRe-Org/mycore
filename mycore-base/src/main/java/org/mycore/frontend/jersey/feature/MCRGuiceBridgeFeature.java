/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.jersey.feature;

import java.util.Objects;

import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.mycore.common.inject.MCRInjectorConfig;

import com.google.inject.Injector;

public class MCRGuiceBridgeFeature implements Feature {

    private final ServiceLocator scopedLocator;

    @Inject
    private MCRGuiceBridgeFeature(ServiceLocator locator) {
        this.scopedLocator = locator;
    }

    @Override
    public boolean configure(FeatureContext context) {
        Objects.requireNonNull(scopedLocator, "HK2 ServiceLocator was not injected");
        Injector injector = MCRInjectorConfig.injector();
        GuiceBridge.getGuiceBridge().initializeGuiceBridge(scopedLocator);
        GuiceIntoHK2Bridge guiceBridge = scopedLocator.getService(GuiceIntoHK2Bridge.class);
        guiceBridge.bridgeGuiceInjector(injector);
        LogManager.getLogger().info("Initialize hk2 - guice bridge...done");
        return true;
    }
}
