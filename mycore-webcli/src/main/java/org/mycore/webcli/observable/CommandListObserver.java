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
        if (observable.equals(obCommandList) && session.isOpen()) {
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
