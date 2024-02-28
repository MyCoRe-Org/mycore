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

package org.mycore.resource.selector;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.MCRRuntimeComponentDetector;
import org.mycore.common.config.MCRRuntimeComponentDetector.ComponentOrder;
import org.mycore.common.hint.MCRHints;

/**
 * A {@link MCRHighestComponentPriorityResourceSelector} is a {@link MCRResourceSelector} that prioritizes
 * resources by module priority.
 * <p>
 * To accomplish this as efficient as possible, it traverses the list of {@link MCRComponent} returned by
 * {@link MCRRuntimeComponentDetector#getAllComponents(ComponentOrder)} from highest to lowest priority
 * until it finds a component that contains one of the resource candidates, which is selected. After that,
 * it only continues with components of the same priority, to check if they also contain a resource candidate.
 * Such candidates are also selected. If no component contains a resource candidate, no candidate is selected.
 */
public class MCRHighestComponentPriorityResourceSelector extends MCRResourceSelectorBase {

    @Override
    protected List<URL> doSelect(List<URL> resourceUrls, MCRHints hints) {
        int highestPriority = -1;
        List<URL> unmatchedResourceUrls = resourceUrls;
        List<URL> highestPriorityModuleResourceUrls = new LinkedList<>();
        for (MCRComponent component : componentsByComponentPriority()) {
            int priority = component.getPriority();
            getLogger().debug("Testing component " + component.getName() + " with priority " + priority);
            if (highestPriority != -1 && highestPriority != priority) {
                getLogger().debug("Found component with priority lower than selected priority {}, stop looking",
                    highestPriority);
                break;
            }
            String componentUrl = "jar:" + component.getJarFile().toURI();
            getLogger().debug("Comparing component URL {} ... ", componentUrl);
            for (URL resourceUrl : unmatchedResourceUrls) {
                getLogger().debug(" ... with resource URL {}", resourceUrl);
                if (resourceUrl.toString().startsWith(componentUrl)) {
                    getLogger().debug("Found match, using component URL {}", componentUrl);
                    highestPriorityModuleResourceUrls.add(resourceUrl);
                    unmatchedResourceUrls.remove(resourceUrl);
                    if (highestPriority != priority) {
                        getLogger().debug("Selected priority {}, keep looking for components with same priority",
                            priority);
                        highestPriority = priority;
                    }
                    break;
                }
            }
        }
        return highestPriorityModuleResourceUrls;
    }

    private SortedSet<MCRComponent> componentsByComponentPriority() {
        return MCRRuntimeComponentDetector.getAllComponents(ComponentOrder.HIGHEST_PRIORITY_FIRST);
    }

}
