package org.mycore.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class ListeningPriortiyExecutorServiceTest {

    private static Logger LOGGER = LogManager.getLogger(ListeningPriortiyExecutorServiceTest.class);

    static int EXCPECTED[] = { 1, 4, 2, 3 };

    @Test
    public void priortiy() throws Exception {
        MCRListeningPriorityExecutorService es = new MCRListeningPriorityExecutorService(
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>()));

        UnimportantTask unimportantTask1 = new UnimportantTask(1);
        UnimportantTask unimportantTask2 = new UnimportantTask(2);
        UnimportantTask unimportantTask3 = new UnimportantTask(3);
        ImportantTask importantTask = new ImportantTask(4);

        TestCallback callback = new TestCallback();
        ListenableFuture<Integer> submit = es.submit(unimportantTask1);
        Futures.addCallback(submit, callback);
        submit = es.submit(unimportantTask2);
        Futures.addCallback(submit, callback);
        submit = es.submit(unimportantTask3);
        Futures.addCallback(submit, callback);
        submit = es.submit(importantTask);
        Futures.addCallback(submit, callback);
        es.awaitTermination(1, TimeUnit.SECONDS);

        assertEquals("all threads should be executed after termination", 4, TestCallback.COUNTER);
        for (int i = 0; i < 4; i++) {
            assertEquals(
                "threads should be executed in order -> unimportantTask1, importantTask, unimportantTask2, unimportantTask3",
                EXCPECTED[i], TestCallback.ORDER[i]);
        }
    }

    private static class TestCallback implements FutureCallback<Integer> {
        static int COUNTER = 0;

        static int ORDER[] = { 0, 0, 0, 0 };

        @Override
        public void onFailure(Throwable t) {
            assertFalse(t.getMessage(), true);
        }

        @Override
        public void onSuccess(Integer result) {
            ORDER[COUNTER++] = result.intValue();
        }
    }

    private static class ImportantTask implements Callable<Integer>, MCRPrioritizable<Integer> {
        private int id;

        public ImportantTask(Integer id) {
            this.id = id;
        }

        @Override
        public Integer call() throws Exception {
            LOGGER.info("Executing task " + id);
            Thread.sleep(200);
            return id;
        }

        @Override
        public Integer getPriority() {
            return 10;
        }
    }

    private static class UnimportantTask implements Callable<Integer>, MCRPrioritizable<Integer> {
        private int id;

        public UnimportantTask(Integer id) {
            this.id = id;
        }

        @Override
        public Integer call() throws Exception {
            LOGGER.info("Executing task " + id);
            Thread.sleep(200);
            return id;
        }

        @Override
        public Integer getPriority() {
            return 1;
        }
    }

}
