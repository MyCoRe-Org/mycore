/*
 *
 * $Revision$ $Date$
 *
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 *
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
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
 * A {@link MCRCombinedResourceSelector} is a {@link MCRResourceSelector} that delegates to multiple other
 * {@link MCRResourceSelector}, one after another.
 */
@MCRConfigurationProxy(proxyClass = MCRCombinedResourceSelector.Factory.class)
public final class MCRCombinedResourceSelector extends MCRResourceSelectorBase {

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

        @MCRInstanceList(name = "Selectors", valueClass = MCRResourceSelector.class)
        public List<MCRResourceSelector> selectors;

        @Override
        public MCRCombinedResourceSelector get() {
            return new MCRCombinedResourceSelector(selectors);
        }

    }

}
