package org.mycore.util.concurrent;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.mycore.util.concurrent.processing.MCRProcessableFactory;

public class MCRPrioritySupplierTest {

    private static Logger LOGGER = LogManager.getLogger(MCRPrioritySupplierTest.class);

    static int EXCPECTED[] = { 1, 10, 5, 4, 3, 2 };

    @Test
    public void priortiy() throws Exception {
        ThreadPoolExecutor es = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            MCRProcessableFactory.newPriorityBlockingQueue());

        TaskConsumer callback = new TaskConsumer();

        CompletableFuture.supplyAsync(new MCRPrioritySupplier<>(new Task(1), 1), es).thenAccept(callback);
        CompletableFuture.supplyAsync(new MCRPrioritySupplier<>(new Task(2), 2), es).thenAccept(callback);
        CompletableFuture.supplyAsync(new MCRPrioritySupplier<>(new Task(3), 3), es).thenAccept(callback);
        CompletableFuture.supplyAsync(new MCRPrioritySupplier<>(new Task(4), 4), es).thenAccept(callback);
        CompletableFuture.supplyAsync(new MCRPrioritySupplier<>(new Task(5), 5), es).thenAccept(callback);
        CompletableFuture.supplyAsync(new MCRPrioritySupplier<>(new Task(10), 10), es).thenAccept(callback);

        es.awaitTermination(1, TimeUnit.SECONDS);

        assertEquals("all threads should be executed after termination", 6, TaskConsumer.COUNTER);
        assertArrayEquals("threads should be executed in order: " + Arrays.toString(EXCPECTED), EXCPECTED,
            TaskConsumer.ORDER);
    }

    private static class TaskConsumer implements Consumer<Integer> {
        static int COUNTER = 0;

        static int ORDER[] = { 0, 0, 0, 0, 0, 0 };

        @Override
        public void accept(Integer value) {
            ORDER[COUNTER++] = value;
        }

    }

    private static class Task implements Supplier<Integer> {
        private int id;

        public Task(Integer id) {
            this.id = id;
        }

        @Override
        public Integer get() {
            try {
                LOGGER.info("Executing task " + id);
                Thread.sleep(100);
                return id;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }

}
