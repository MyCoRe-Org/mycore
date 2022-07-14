package org.mycore.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

public class MCRPoolTest {

    @Test
    public void acquire() throws InterruptedException {
        int size = Math.max(Runtime.getRuntime().availableProcessors() / 4, 2);
        int runs = size * 10;
        MCRPool<Resource> resourcePool = new MCRPool<>(size, Resource::new);
        final int beforeCounter = Resource.counter;
        Assert.assertNotNull(resourcePool.acquire());
        Assert.assertEquals(beforeCounter + 1, Resource.counter);
        Resource.counter = 0;
        IntStream.range(0, runs)
            .parallel()
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                try {
                    return resourcePool.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }))
            .map(f -> f.thenApply(r -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return r;
            }))
            .map(f -> f.thenAccept(resourcePool::release))
            .forEach(CompletableFuture::join);
        Assert.assertTrue(Resource.counter <= size);
    }

    private static class Resource {
        static int counter = 0;

        public Resource() {
            counter++;
        }
    }
}
