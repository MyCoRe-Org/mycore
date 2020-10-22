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

package org.mycore.webcli.flow;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.Flow;
import java.util.concurrent.locks.ReentrantLock;

import javax.websocket.CloseReason;
import javax.websocket.Session;

import org.apache.logging.log4j.LogManager;

import com.google.gson.JsonObject;

public class MCRJSONSubscriber implements Flow.Subscriber<JsonObject> {

    private final Session session;

    private final ReentrantLock lock;

    private Flow.Subscription subscription;

    public MCRJSONSubscriber(Session session, ReentrantLock lock) {
        this.session = session;
        this.lock = lock;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(JsonObject item) {
        if (!this.session.isOpen()) {
            LogManager.getLogger().warn("Session {} closed, cancel further log event subscription!",
                this.session.getId());
            subscription.cancel();
        }
        LogManager.getLogger().debug(() -> "Sending json: " + item.toString());
        lock.lock();
        try {
            session.getBasicRemote().sendText(item.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            lock.unlock();
            subscription.request(1);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (this.session.isOpen()) {
            try {
                LogManager.getLogger().error("Close session " + session.getId(), throwable);
                this.session.close(new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, throwable.getMessage()));
            } catch (IOException e) {
                LogManager.getLogger().warn("Error in Publisher or Subscriber.", throwable);
            }
        }
    }

    @Override
    public void onComplete() {
        LogManager.getLogger().info("Finished sending JSON.");
    }

    public void cancel() {
        this.subscription.cancel();
    }
}
