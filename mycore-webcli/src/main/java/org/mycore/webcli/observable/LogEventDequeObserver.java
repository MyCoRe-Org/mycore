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
        if (observable == obLogEventDeque) {
            if (sendMessages && session.isOpen()) {
                try {
                    sendAsMessage(obLogEventDeque);
                } catch (IOException ex) {
                    LOGGER.error("Cannot send message to client.", ex);
                }
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
