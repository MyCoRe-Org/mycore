package org.mycore.services.http;

import java.util.concurrent.TimeUnit;

import org.apache.http.conn.HttpClientConnectionManager;

/**
 * Monitors a {@link HttpClientConnectionManager} for expired or idle connections.
 * 
 * These connections will be closed every 5 seconds.
 * @author Thomas Scheffler (yagee)
 */
public class MCRIdleConnectionMonitorThread extends Thread {
    private final HttpClientConnectionManager connMgr;

    private volatile boolean shutdown;

    public MCRIdleConnectionMonitorThread(HttpClientConnectionManager connMgr) {
        super();
        this.connMgr = connMgr;
    }

    @Override
    public void run() {
        try {
            while (!shutdown) {
                synchronized (this) {
                    wait(5000);
                    // Close expired connections
                    connMgr.closeExpiredConnections();
                    // Close inactive connection
                    connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
                }
            }
        } catch (InterruptedException ex) {
            // terminate
        }
    }

    public void shutdown() {
        shutdown = true;
        synchronized (this) {
            notifyAll();
        }
    }

}
