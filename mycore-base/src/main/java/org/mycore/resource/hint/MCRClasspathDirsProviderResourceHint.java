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

package org.mycore.resource.hint;

import java.util.Optional;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.hint.MCRHint;
import org.mycore.common.hint.MCRHintKey;
import org.mycore.resource.common.MCRClasspathDirsProvider;

/**
 * A {@link MCRClasspathDirsProviderResourceHint} is a {@link MCRHint} for
 * {@link MCRResourceHintKeys#CLASSPATH_DIRS_PROVIDER} that provides an optionally configured
 * {@link MCRClasspathDirsProvider}
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRClasspathDirsProviderResourceHint#PROVIDER_KEY} can be used to
 * specify the classpath directories provider to be used.
 * <li> For the classpath directories provider, the property suffix {@link MCRSentinel#ENABLED_KEY} can be used to
 * exclude the directories provider from the configuration.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.hint.MCRClasspathDirsProviderResourceHint
 * [...].Provider.Class=foo.bar.FooProvider
 * [...].Provider.Enabled=true
 * [...].Provider.Key1=Value1
 * [...].Provider.Key2=Value2
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRClasspathDirsProviderResourceHint.Factory.class)
public final class MCRClasspathDirsProviderResourceHint implements MCRHint<MCRClasspathDirsProvider> {

    public static final String PROVIDER_KEY = "Provider";

    private final MCRClasspathDirsProvider provider;

    public MCRClasspathDirsProviderResourceHint(MCRClasspathDirsProvider provider) {
        this.provider = provider;
    }

    @Override
    public MCRHintKey<MCRClasspathDirsProvider> key() {
        return MCRResourceHintKeys.CLASSPATH_DIRS_PROVIDER;
    }

    @Override
    public Optional<MCRClasspathDirsProvider> value() {
        return Optional.ofNullable(provider);
    }

    public static class Factory implements Supplier<MCRClasspathDirsProviderResourceHint> {

        @MCRInstance(name = PROVIDER_KEY, valueClass = MCRClasspathDirsProvider.class, required = false,
            sentinel = @MCRSentinel)
        public MCRClasspathDirsProvider provider;

        @Override
        public MCRClasspathDirsProviderResourceHint get() {
            return new MCRClasspathDirsProviderResourceHint(provider);
        }

    }
}
