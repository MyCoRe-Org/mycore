package org.mycore.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class ListeningPriortiyExecutorServiceTest {

    @Test
    public void priortiy() throws Exception {
        MCRListeningPriorityExecutorService es = new MCRListeningPriorityExecutorService(new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<Runnable>()));
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
    }

    private static class TestCallback implements FutureCallback<Integer> {
        static int counter = 0;

        static int excpected[] = { 1, 4, 2, 3 };

        @Override
        public void onFailure(Throwable t) {
            assertFalse(t.getMessage(), true);
        }

        @Override
        public void onSuccess(Integer result) {
            assertEquals("Threads should be executed in order -> unimportantTask1, importantTask, unimportantTask2, unimportantTask3",
                    excpected[counter++], result.intValue());
        }
    }

    private static class ImportantTask implements Callable<Integer>, MCRPrioritizable<Integer> {
        private int id;

        public ImportantTask(Integer id) {
            this.id = id;
        }

        @Override
        public Integer call() throws Exception {
            Thread.sleep(100);
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
            Thread.sleep(100);
            return id;
        }

        @Override
        public Integer getPriority() {
            return 1;
        }
    }

}
