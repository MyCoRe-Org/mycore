package org.mycore.webcli.observable;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import javax.websocket.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

/**
 * @author Michel Buechner (mcrmibue)
 * 
 */
public class CommandListObserver implements Observer {

    private ObservableCommandList obCommandList;

    private Session session;

    private static final Logger LOGGER = LogManager.getLogger();

    public CommandListObserver(ObservableCommandList obCommandList, Session session) {
        this.obCommandList = obCommandList;
        this.session = session;
    }

    public void update(Observable observable, Object obj) {
        if (observable == obCommandList && session.isOpen()) {
            JsonObject jObject = new JsonObject();
            jObject.addProperty("type", "commandQueue");
            if (obCommandList.isEmpty()) {
                jObject.addProperty("return", "");
            } else {
                jObject.addProperty("return", obCommandList.getAsJSONArrayString());
            }
            jObject.addProperty("size", obCommandList.size());
            try {
                this.session.getBasicRemote().sendText(jObject.toString());
            } catch (IOException ex) {
                LOGGER.error("Cannot send message to client.", ex);
            }
        }
    }

    public void changeSession(Session session) {
        this.session = session;
    }
}
