/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class MCRCommandListProcessorTest {

    @Test
    void serializesAtMostOneHundredCommandsWhilePreservingQueueSize() {
        MCRCommandListProcessor processor = new MCRCommandListProcessor();
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        processor.onSubscribe(new NoOpSubscription());
        processor.subscribe(new ResultSubscriber(result));

        try {
            processor.onNext(new BoundedTraversalCollection(10_000, 100));
            JsonObject json = result.orTimeout(5, TimeUnit.SECONDS).join();
            JsonArray commands = json.getAsJsonArray("return");

            assertEquals(10_000, json.get("size").getAsInt());
            assertEquals(100, commands.size());
            assertEquals("command 0", commands.get(0).getAsString());
            assertEquals("command 99", commands.get(99).getAsString());
        } finally {
            processor.close();
        }
    }

    private static class BoundedTraversalCollection extends AbstractCollection<String> {

        private final int size;

        private final int traversalLimit;

        BoundedTraversalCollection(int size, int traversalLimit) {
            this.size = size;
            this.traversalLimit = traversalLimit;
        }

        @Override
        public Iterator<String> iterator() {
            return new Iterator<>() {

                private int index;

                @Override
                public boolean hasNext() {
                    return index < size;
                }

                @Override
                public String next() {
                    if (index >= traversalLimit) {
                        throw new AssertionError("Processor traversed more commands than it serializes");
                    }
                    return "command " + index++;
                }
            };
        }

        @Override
        public int size() {
            return size;
        }

    }

    private record ResultSubscriber(CompletableFuture<JsonObject> result) implements Flow.Subscriber<JsonObject> {

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(1);
        }

        @Override
        public void onNext(JsonObject item) {
            result.complete(item);
        }

        @Override
        public void onError(Throwable throwable) {
            result.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            // nothing to do
        }

    }

    private static class NoOpSubscription implements Flow.Subscription {

        @Override
        public void request(long count) {
            // nothing to do
        }

        @Override
        public void cancel() {
            // nothing to do
        }

    }

}
