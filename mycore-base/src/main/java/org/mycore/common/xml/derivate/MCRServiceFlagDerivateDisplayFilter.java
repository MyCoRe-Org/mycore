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

package org.mycore.common.xml.derivate;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.metadata.MCRDerivate;

/**
 * A {@link MCRServiceFlagDerivateDisplayFilter} is a {@link MCRDerivateDisplayFilter} that looks up explicitly set
 * service flags.
 * <p>
 * The service flags for an intent must have type <code>{flag-type-prefix}-<em>{intent}</em></code> and a boolean value.
 * <p>
 * If a derivate has more than one service flag for a given intent, the last such flag is used.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRServiceFlagDerivateDisplayFilter#FLAG_TYPE_PREFIX_KEY} can be used to
 * specify the flag type prefix to be used.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.common.xml.derivate.MCRServiceFlagDerivateDisplayFilter
 * [...].FlagTypePrefix=intent
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRServiceFlagDerivateDisplayFilter.Factory.class)
public class MCRServiceFlagDerivateDisplayFilter implements MCRDerivateDisplayFilter {

    public static final String DEFAULT_KEY_PREFIX = MCRDerivateDisplayFilter.DEFAULT_KEY_PREFIX + "ServiceFlag.";

    public static final String FLAG_TYPE_PREFIX_KEY = "FlagTypePrefix";

    private final String flagTypePrefix;

    public MCRServiceFlagDerivateDisplayFilter(String flagTypePrefix) {
        this.flagTypePrefix = Objects.requireNonNull(flagTypePrefix, "Flag type prefix must not be null");
    }

    @Override
    public Boolean isDisplayEnabled(MCRDerivate derivate, String intent) {

        String flagType = flagTypePrefix + "-" + intent;
        List<String> flagValues = derivate.getService().getFlags(flagType);

        if (!flagValues.isEmpty()) {
            return Boolean.parseBoolean(flagValues.getLast());
        }

        return null;

    }

    public static class Factory implements Supplier<MCRServiceFlagDerivateDisplayFilter> {

        @MCRProperty(name = FLAG_TYPE_PREFIX_KEY, defaultName = DEFAULT_KEY_PREFIX + FLAG_TYPE_PREFIX_KEY)
        public String flagTypePrefix;

        @Override
        public MCRServiceFlagDerivateDisplayFilter get() {
            return new MCRServiceFlagDerivateDisplayFilter(flagTypePrefix);
        }

    }

}
