package org.mycore.frontend.editor;

import java.util.concurrent.atomic.AtomicLong;

public class MCRUniqueID {

    private static AtomicLong counter = new AtomicLong(System.nanoTime());

    public static String buildID() {
        long nextValue = counter.incrementAndGet();
        String base36 = Long.toString(nextValue, 36);
        String newID = new StringBuffer(base36).reverse().toString();
        return newID;
    }
}
