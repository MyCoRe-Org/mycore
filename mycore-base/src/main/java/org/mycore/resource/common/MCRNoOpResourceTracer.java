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

public class MCRNoOpResourceTracer implements MCRResourceTracer {

    @Override
    public MCRResourceTracer update(Object context) {
        return this;
    }

    @Override
    public MCRResourceTracer update(Object context, String info) {
        return this;
    }

    @Override
    public void trace(Supplier<String> onTrace) {
    }

    @Override
    public void trace(MCRHints hints, Consumer<Appender> onTrace) {
    }

    @Override
    public <T> T trace(MCRHints hints, T value, BiConsumer<Appender, T> onTrace) {
        return value;
    }

    @Override
    public <T> Stream<T> traceStream(MCRHints hints, Stream<T> values, BiConsumer<Appender, List<T>> onTrace) {
        return values;
    }

    @Override
    public String logMessage(String introduction) {
        return introduction;
    }

}
