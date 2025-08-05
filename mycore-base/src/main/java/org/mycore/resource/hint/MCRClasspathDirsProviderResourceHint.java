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

@MCRConfigurationProxy(proxyClass = MCRClasspathDirsProviderResourceHint.Factory.class)
public final class MCRClasspathDirsProviderResourceHint implements MCRHint<MCRClasspathDirsProvider> {

    public static final String CLASSPATH_DIRS_PROVIDER_KEY = "ClasspathDirsProvider";

    private final MCRClasspathDirsProvider classpathDirsProvider;

    public MCRClasspathDirsProviderResourceHint(MCRClasspathDirsProvider classpathDirsProvider) {
        this.classpathDirsProvider = classpathDirsProvider;
    }

    @Override
    public MCRHintKey<MCRClasspathDirsProvider> key() {
        return MCRResourceHintKeys.CLASSPATH_DIRS_PROVIDER;
    }

    @Override
    public Optional<MCRClasspathDirsProvider> value() {
        return Optional.ofNullable(classpathDirsProvider);
    }

    public static class Factory implements Supplier<MCRClasspathDirsProviderResourceHint> {

        @MCRInstance(name = CLASSPATH_DIRS_PROVIDER_KEY, valueClass = MCRClasspathDirsProvider.class, required = false,
            sentinel = @MCRSentinel)
        public MCRClasspathDirsProvider classpathDirsProvider;

        @Override
        public MCRClasspathDirsProviderResourceHint get() {
            return new MCRClasspathDirsProviderResourceHint(classpathDirsProvider);
        }

    }
}
