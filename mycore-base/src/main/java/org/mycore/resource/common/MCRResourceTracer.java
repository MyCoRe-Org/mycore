package org.mycore.resource.common;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.mycore.common.hint.MCRHints;

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
