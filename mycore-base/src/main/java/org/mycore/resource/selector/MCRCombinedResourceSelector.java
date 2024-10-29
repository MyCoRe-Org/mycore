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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;

/**
 * {@link MCRCombinedResourceSelector} is an implementation of {@link MCRResourceSelector} that delegates to multiple
 * other {@link MCRResourceSelector} instances, one after another.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> Selectors are configured as a list using the property suffix {@link MCRCombinedResourceSelector#SELECTORS_KEY}.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.resource.selector.MCRCombinedResourceSelector
 * [...].Providers.10.Class=foo.bar.FooSelector
 * [...].Providers.10.Key1=Value1
 * [...].Providers.10.Key2=Value2
 * [...].Providers.20.Class=foo.bar.BarSelector
 * [...].Providers.20.Key1=Value1
 * [...].Providers.20.Key2=Value2
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRCombinedResourceSelector.Factory.class)
public final class MCRCombinedResourceSelector extends MCRResourceSelectorBase {

    public static final String SELECTORS_KEY = "Selectors";

    private final List<MCRResourceSelector> selectors;

    public MCRCombinedResourceSelector(MCRResourceSelector... selectors) {
        this(Arrays.asList(selectors));
    }

    public MCRCombinedResourceSelector(List<MCRResourceSelector> selectors) {
        this.selectors = new ArrayList<>(Objects.requireNonNull(selectors));
        this.selectors.forEach(Objects::requireNonNull);
    }

    @Override
    protected List<URL> doSelect(List<URL> resourceUrls, MCRHints hints) {
        List<URL> selectedResourceUrls = resourceUrls;
        for (MCRResourceSelector selector : selectors) {
            if (selectedResourceUrls.size() > 1) {
                selectedResourceUrls = selector.select(selectedResourceUrls, hints);
            } else {
                break;
            }
        }
        return selectedResourceUrls;
    }

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = super.compileDescription(level);
        selectors.forEach(selector -> description.add("Selector", selector.compileDescription(level)));
        return description;
    }

    public static class Factory implements Supplier<MCRCombinedResourceSelector> {

        @MCRInstanceList(name = SELECTORS_KEY, valueClass = MCRResourceSelector.class)
        public List<MCRResourceSelector> selectors;

        @Override
        public MCRCombinedResourceSelector get() {
            return new MCRCombinedResourceSelector(selectors);
        }

    }

}
