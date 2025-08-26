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

package org.mycore.resource.common;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.mycore.common.hint.MCRHints;
import org.mycore.resource.MCRResourceResolver;

/**
 * A {@link MCRResourceTracer} implements a strategy to compile information about the process
 * of resolving a resource in {@link MCRResourceResolver}.
 */
public interface MCRResourceTracer {

    MCRResourceTracer update(Object context);

    MCRResourceTracer update(Object context, String info);

    void trace(Supplier<String> onTrace);

    void trace(MCRHints hints, Consumer<Appender> onTrace);

    <T> T trace(MCRHints hints, T value, BiConsumer<Appender, T> onTrace);

    <T> Stream<T> traceStream(MCRHints hints, Stream<T> values, BiConsumer<Appender, List<T>> onTrace);

    String logMessage(String introduction);

    interface Appender {
        void append(String message);
    }

}
