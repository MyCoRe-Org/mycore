/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
        this.logEventDeque = new ConcurrentLinkedDeque<>();
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
