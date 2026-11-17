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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;

import com.google.gson.JsonObject;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;

public class MCRJSONSubscriber implements Flow.Subscriber<JsonObject> {

    private final Session session;

    private final ReentrantLock lock;

    private Flow.Subscription subscription;

    private final CompletableFuture<Void> completion = new CompletableFuture<>();

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
            cancel();
            return;
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
        completion.complete(null);
    }

    @Override
    public void onComplete() {
        LogManager.getLogger().info("Finished sending JSON.");
        completion.complete(null);
    }

    public void cancel() {
        this.subscription.cancel();
        completion.complete(null);
    }

    /**
     * Blocks until this subscriber has completed, which happens on {@link #onComplete()},
     * {@link #onError(Throwable)} or {@link #cancel()}.
     * <p>
     * If the timeout elapses first, the method returns silently and only logs a warning.
     * If the waiting thread is interrupted, the interrupt flag is restored and the method returns.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     */
    public void awaitCompletion(long timeout, TimeUnit unit) {
        try {
            completion.get(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            LogManager.getLogger().warn("Not all messages could be sent to client of session {}.",
                this.session.getId());
        }
    }
}
