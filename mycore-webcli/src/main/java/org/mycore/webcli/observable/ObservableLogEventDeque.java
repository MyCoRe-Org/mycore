package org.mycore.webcli.observable;

import java.util.Observable;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.logging.log4j.core.LogEvent;

/**
 * @author Michel Buechner (mcrmibue)
 * 
 */
public class ObservableLogEventDeque extends Observable {

    ConcurrentLinkedDeque<LogEvent> logEventDeque;

    public ObservableLogEventDeque() {
        this.logEventDeque = new ConcurrentLinkedDeque<LogEvent>();
    }

    public void add(LogEvent event) {
        logEventDeque.add(event);
        setChanged();
        notifyObservers();
    }

    public void clear() {
        logEventDeque.clear();
        setChanged();
        notifyObservers();
    }

    public boolean isEmpty() {
        return logEventDeque.isEmpty();
    }

    public LogEvent poll() {
        LogEvent event = logEventDeque.poll();
        setChanged();
        notifyObservers();
        return event;
    }

    public LogEvent pollLast() {
        LogEvent event = logEventDeque.pollLast();
        setChanged();
        notifyObservers();
        return event;
    }
}
