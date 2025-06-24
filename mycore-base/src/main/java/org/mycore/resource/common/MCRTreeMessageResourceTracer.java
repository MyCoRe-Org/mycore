package org.mycore.resource.common;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;

public class MCRTreeMessageResourceTracer implements MCRResourceTracer {

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
