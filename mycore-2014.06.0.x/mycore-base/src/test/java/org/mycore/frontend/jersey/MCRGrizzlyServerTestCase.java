package org.mycore.frontend.jersey;

import org.junit.After;
import org.junit.Before;
import org.mycore.frontend.jersey.server.MCRGrizzlyTestServer;

public abstract class MCRGrizzlyServerTestCase {
    private MCRGrizzlyTestServer testServer;

    @Before
    public void setup() {
        setTestServer(new MCRGrizzlyTestServer(getPackageName()));
        getTestServer().start();
    }

    @After
    public void cleanUp() {
        getTestServer().stop();
    }

    protected String getPackageName() {
        return getClass().getPackage().getName();
    }

    public void setTestServer(MCRGrizzlyTestServer testServer) {
        this.testServer = testServer;
    }

    public MCRGrizzlyTestServer getTestServer() {
        return this.testServer;
    }
}
