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
