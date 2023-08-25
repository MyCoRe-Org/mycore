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

package org.mycore.common.resource.locator;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.resource.MCRResourcePath;
import org.mycore.common.resource.provider.MCRResourceProvider;
import org.mycore.common.resource.provider.MCRResourceProvider.PrefixStripper;

/**
 * A {@link MCRCombinedResourceLocator} is a {@link MCRResourceLocator} that delegates to multiple other
 * {@link MCRResourceLocator}, one after another.
 */
@MCRConfigurationProxy(proxyClass = MCRCombinedResourceLocator.Factory.class)
public class MCRCombinedResourceLocator extends MCRResourceLocatorBase {

    private final List<MCRResourceLocator> locators;

    public MCRCombinedResourceLocator(MCRResourceLocator... locators) {
        this(Arrays.asList(locators));
    }

    public <T> MCRCombinedResourceLocator(List<MCRResourceLocator> locators) {
        this.locators = new ArrayList<>(Objects.requireNonNull(locators));
        this.locators.forEach(Objects::requireNonNull);
        Collections.reverse(this.locators);
    }

    @Override
    protected Stream<URL> doLocate(MCRResourcePath path, MCRHints hints) {
        Stream<URL> resourceUrls = Stream.empty();
        for (MCRResourceLocator locator : locators) {
            resourceUrls = Stream.concat(locator.locate(path, hints), resourceUrls);
        }
        return resourceUrls;
    }

    @Override
    public Set<MCRResourceProvider.PrefixStripper> prefixPatterns(MCRHints hints) {
        Set<PrefixStripper> strippers = new HashSet<>();
        locators.forEach(locator -> strippers.addAll(locator.prefixPatterns(hints)));
        return strippers;
    }

    public static class Factory implements Supplier<MCRCombinedResourceLocator> {

        @MCRInstanceList(name = "Locators", valueClass = MCRResourceLocator.class)
        public List<MCRResourceLocator> locators;

        @Override
        public MCRCombinedResourceLocator get() {
            return new MCRCombinedResourceLocator(locators);
        }

    }

}
