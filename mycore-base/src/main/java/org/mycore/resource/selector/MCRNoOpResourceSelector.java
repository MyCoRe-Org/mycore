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
import java.util.List;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.hint.MCRHints;

/**
 * A {@link MCRNoOpResourceSelector} is a {@link MCRResourceSelector} that doesn't select resources.
 * <p>
 * No configuration options are available.
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.selector.MCRNoOpResourceSelector
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRNoOpResourceSelector.Factory.class)
public class MCRNoOpResourceSelector extends MCRResourceSelectorBase {

    @Override
    protected List<URL> doSelect(List<URL> resourceUrls, MCRHints hints) {
        return resourceUrls;
    }

    public static class Factory implements Supplier<MCRNoOpResourceSelector> {

        @Override
        public MCRNoOpResourceSelector get() {
            return new MCRNoOpResourceSelector();
        }

    }

}
