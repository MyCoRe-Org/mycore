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
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.MCRResourceResolver;

/**
 * A {@link MCRTreeMessageResourceTracer} is a {@link MCRResourceTracer} that information about the process of
 * resolving a resource in {@link MCRResourceResolver} into a {@link MCRTreeMessage}.
 */
public final class MCRTreeMessageResourceTracer implements MCRResourceTracer {

    private final MCRTreeMessage message;

    private MCRTreeMessageResourceTracer(MCRTreeMessage message) {
        this.message = message;
    }

    public MCRTreeMessageResourceTracer() {
        this(new MCRTreeMessage());
    }

    @Override
    public MCRResourceTracer update(Object context) {
        MCRTreeMessage childMessage = new MCRTreeMessage();
        message.add(context.getClass().getSimpleName(), childMessage);
        return new MCRTreeMessageResourceTracer(childMessage);
    }

    @Override
    public MCRResourceTracer update(Object context, String info) {
        MCRTreeMessage childMessage = new MCRTreeMessage();
        message.add(context.getClass().getSimpleName() + " [" + info + "]", childMessage);
        return new MCRTreeMessageResourceTracer(childMessage);
    }

    @Override
    public void trace(Supplier<String> onTrace) {
        message.add(onTrace.get());
    }

    @Override
    public void trace(MCRHints hints, Consumer<Appender> onTrace) {
        onTrace.accept(message::add);
    }

    @Override
    public <T> T trace(MCRHints hints, T value, BiConsumer<Appender, T> onTrace) {
        onTrace.accept(message::add, value);
        return value;
    }

    @Override
    public <T> Stream<T> traceStream(MCRHints hints, Stream<T> values, BiConsumer<Appender, List<T>> onTrace) {
        List<T> listValues = values.toList();
        onTrace.accept(message::add, listValues);
        values = listValues.stream();
        return values;
    }

    @Override
    public String logMessage(String introduction) {
        return message.logMessage(introduction);
    }

}
