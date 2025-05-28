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
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.hint.MCRResourceHintKeys;

public final class MCRTraceLoggingHelper {

    private MCRTraceLoggingHelper() {
    }

    public static MCRHints init(MCRHints hints) {
        MCRTreeMessage message = new MCRTreeMessage();
        MCRHintsBuilder builder = hints.builder();
        builder.add(MCRResourceHintKeys.TRACE_TREE_MESSAGE, message);
        return builder.build();
    }

    public static MCRHints update(MCRHints hints, Object context, String info) {
        return hints.get(MCRResourceHintKeys.TRACE_TREE_MESSAGE).map(message -> {
            MCRTreeMessage childMessage = new MCRTreeMessage();
            message.add(toMessageKey(context, info), childMessage);
            return hints.builder().add(MCRResourceHintKeys.TRACE_TREE_MESSAGE, childMessage).build();
        }).orElse(hints);
    }

    private static String toMessageKey(Object context, String info) {
        return context.getClass().getSimpleName() + (info != null ? " [" + info + "]" : "");
    }

    public static void trace(MCRHints hints, Supplier<String> onTrace) {
        MCRTreeMessage message = hints.getOrElse(MCRResourceHintKeys.TRACE_TREE_MESSAGE, null);
        if (message != null) {
            message.add(onTrace.get());
        }
    }

    public static void trace(MCRHints hints, Consumer<MCRTreeMessage> onTrace) {
        MCRTreeMessage message = hints.getOrElse(MCRResourceHintKeys.TRACE_TREE_MESSAGE, null);
        if (message != null) {
            onTrace.accept(message);
        }
    }

    public static <T> T trace(MCRHints hints, T value, BiConsumer<MCRTreeMessage, T> onTrace) {
        MCRTreeMessage message = hints.getOrElse(MCRResourceHintKeys.TRACE_TREE_MESSAGE, null);
        if (message != null) {
            onTrace.accept(message, value);
        }
        return value;
    }

    public static <T> Stream<T> traceStream(MCRHints hints, Stream<T> values,
        BiConsumer<MCRTreeMessage, List<T>> onTrace) {
        MCRTreeMessage message = hints.getOrElse(MCRResourceHintKeys.TRACE_TREE_MESSAGE, null);
        if (message != null) {
            List<T> listValues = values.toList();
            onTrace.accept(message, listValues);
            values = listValues.stream();
        }
        return values;
    }

    public static MCRTreeMessage get(MCRHints hints) {
        return hints.get(MCRResourceHintKeys.TRACE_TREE_MESSAGE).orElse(new MCRTreeMessage());
    }

}
