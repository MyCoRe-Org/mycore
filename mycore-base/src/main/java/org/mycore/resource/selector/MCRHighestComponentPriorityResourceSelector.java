/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.MCRRuntimeComponentDetector;
import org.mycore.common.config.MCRRuntimeComponentDetector.ComponentOrder;
import org.mycore.common.hint.MCRHints;
import org.mycore.resource.common.MCRResourceTracer;
import org.mycore.resource.hint.MCRResourceHintKeys;

/**
 * A {@link MCRHighestComponentPriorityResourceSelector} is a {@link MCRResourceSelector} that prioritizes
 * resources by module priority.
 * <p>
 * To accomplish this as efficient as possible, it traverses the list of {@link MCRComponent} returned by
 * {@link MCRRuntimeComponentDetector#getAllComponents(ComponentOrder)} from highest to lowest priority
 * until it finds a component that contains one of the resource candidates, which is selected. After that,
 * it only continues with components of the same priority, to check if they also contain a resource candidate.
 * Such candidates are also selected. If no component contains a resource candidate, no candidate is selected.
 * <p>
 * It uses the set of {@link MCRComponent} instances hinted at by {@link MCRResourceHintKeys#COMPONENTS}, if present.
 * <p>
 * No configuration options are available.
 * <p>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.selector.MCRHighestComponentPriorityResourceSelector
 * </code></pre>
 */
@SuppressWarnings("PMD.GuardLogStatement")
public final class MCRHighestComponentPriorityResourceSelector extends MCRResourceSelectorBase {

    @Override
    protected List<URL> doSelect(List<URL> resourceUrls, MCRHints hints, MCRResourceTracer tracer) {
        int highestPriority = -1;
        List<URL> unmatchedResourceUrls = new ArrayList<>(resourceUrls);
        List<URL> highestPriorityModuleResourceUrls = new LinkedList<>();
        for (MCRComponent component : componentsByComponentPriority(hints)) {
            int priority = component.getPriority();
            tracer.trace(() -> "Testing component " + component.getName() + " with priority " + priority);
            if (highestPriority != -1 && highestPriority != priority) {
                int highestPrioritySoFar = highestPriority;
                tracer.trace(() -> "Found component with priority lower than "
                    + highestPrioritySoFar + ", stop looking");
                break;
            }
            String componentUrl = "jar:" + component.getJarFile().toURI();
            tracer.trace(() -> "Looking for component URL prefix " + componentUrl + " ...");
            for (URL resourceUrl : unmatchedResourceUrls) {
                tracer.trace(() -> "... in resource URL " + resourceUrl);
                if (matches(resourceUrl.toString(), componentUrl)) {
                    tracer.trace(() -> "Found match, using component URL " + componentUrl);
                    highestPriorityModuleResourceUrls.add(resourceUrl);
                    unmatchedResourceUrls.remove(resourceUrl);
                    if (highestPriority != priority) {
                        tracer.trace(() -> "Selected priority " + priority
                            + ", keep looking for components with same priority");
                        highestPriority = priority;
                    }
                    break;
                }
            }
        }
        return highestPriorityModuleResourceUrls;
    }

    private static boolean matches(String resourceUrl, String componentUrl) {
        return resourceUrl.startsWith(componentUrl) && resourceUrl.charAt(componentUrl.length()) == '!';
    }

    private SortedSet<MCRComponent> componentsByComponentPriority(MCRHints hints) {
        return hints.get(MCRResourceHintKeys.COMPONENTS).orElseGet(TreeSet::new);
    }

}
