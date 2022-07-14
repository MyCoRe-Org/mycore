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
