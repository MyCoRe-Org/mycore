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

package org.mycore.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

public class MCRPoolTest {

    @Test
    public void acquire() throws InterruptedException {
        int size = Math.max(Runtime.getRuntime().availableProcessors() / 4, 2);
        int runs = size * 10;
        MCRPool<ResourceHelper> resourcePool = new MCRPool<>(size, ResourceHelper::new);
        final int beforeCounter = ResourceHelper.counter.get();
        Assert.assertNotNull(resourcePool.acquire());
        Assert.assertEquals(beforeCounter + 1, ResourceHelper.counter.get());
        ResourceHelper.counter.set(0);
        IntStream.range(0, runs)
            .parallel()
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                try {
                    return resourcePool.acquire();
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
            }))
            .map(f -> f.thenApply(r -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
                return r;
            }))
            .map(f -> f.thenAccept(resourcePool::release))
            .forEach(CompletableFuture::join);
        Assert.assertTrue(ResourceHelper.counter.get() <= size);
    }

    private static class ResourceHelper {
        static AtomicInteger counter = new AtomicInteger();

        ResourceHelper() {
            counter.incrementAndGet();
        }
    }
}
