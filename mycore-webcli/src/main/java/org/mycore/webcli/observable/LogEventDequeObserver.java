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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Observable;
import java.util.Observer;

import javax.websocket.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;

import com.google.gson.JsonObject;

/**
 * @author Michel Buechner (mcrmibue)
 * 
 */
public class LogEventDequeObserver implements Observer {

    private ObservableLogEventDeque obLogEventDeque;

    private Session session;

    private boolean sendMessages;

    private static final Logger LOGGER = LogManager.getLogger();

    public LogEventDequeObserver(ObservableLogEventDeque obsLogEventQueue, Session session) {
        this.obLogEventDeque = obsLogEventQueue;
        this.session = session;
        this.sendMessages = true;
    }

    public void update(Observable observable, Object obj) {
        if (observable.equals(obLogEventDeque) && sendMessages && session.isOpen()) {
            try {
                sendAsMessage(obLogEventDeque);
            } catch (IOException ex) {
                LOGGER.error("Cannot send message to client.", ex);
            }
        }
    }

    private void sendAsMessage(ObservableLogEventDeque events) throws IOException {
        JsonObject jObject = new JsonObject();
        jObject.addProperty("type", "log");
        jObject.addProperty("return", getJSONLogAsString(events));
        session.getBasicRemote().sendText(jObject.toString());
    }

    private String getJSONLogAsString(ObservableLogEventDeque events) {
        if (!events.isEmpty()) {
            LogEvent event = events.pollLast();
            JsonObject json = new JsonObject();
            json.addProperty("logLevel", event.getLevel().toString());
            json.addProperty("message", event.getMessage().getFormattedMessage());
            String exception = null;
            if (event.getThrownProxy() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                event.getThrownProxy().getThrowable().printStackTrace(pw);
                pw.close();
                exception = sw.toString();
            }
            json.addProperty("exception", exception);
            json.addProperty("time", event.getTimeMillis());
            return json.toString();
        }
        return "";
    }

    public void changeSession(Session session) {
        this.session = session;
    }

    public void stopSendMessages() {
        this.sendMessages = false;
    }

    public void startSendMessages() {
        this.sendMessages = true;
        try {
            sendAsMessage(obLogEventDeque);
        } catch (IOException ex) {
            LOGGER.error("Cannot send message to client.", ex);
        }
    }
}
