package org.mycore.solr.proxy;

import java.util.concurrent.TimeUnit;

import org.apache.http.conn.ClientConnectionManager;

class MCRIdleConnectionMonitorThread extends Thread {
    private final ClientConnectionManager connMgr;

    private volatile boolean shutdown;

    public MCRIdleConnectionMonitorThread(ClientConnectionManager connMgr) {
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