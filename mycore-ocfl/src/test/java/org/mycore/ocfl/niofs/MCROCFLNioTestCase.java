package org.mycore.ocfl.niofs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.ocfl.MCROCFLTestCase;
import org.mycore.ocfl.MCROCFLTestCaseHelper;
import org.mycore.ocfl.util.MCROCFLObjectIDPrefixHelper;

@RunWith(Parameterized.class)
public abstract class MCROCFLNioTestCase extends MCROCFLTestCase {

    /**
     * This ocfl object is created on test startup.
     */
    protected static final String DERIVATE_1 = "junit_derivate_00000001";

    protected static final String DERIVATE_1_OBJECT_ID = MCROCFLObjectIDPrefixHelper.MCRDERIVATE + DERIVATE_1;

    /**
     * This ocfl object is not created on test startup.
     */
    protected static final String DERIVATE_2 = "junit_derivate_00000002";

    protected static final String DERIVATE_2_OBJECT_ID = MCROCFLObjectIDPrefixHelper.MCRDERIVATE + DERIVATE_2;

    private final boolean remote;

    @Parameterized.Parameters(name = "remote: {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] { { false }, { true } });
    }

    public MCROCFLNioTestCase(boolean remote) {
        this.remote = remote;
    }

    @Override
    protected Map<String, String> getTestProperties() {
        HashMap<String, String> properties = new HashMap<>();
        properties.put("MCR.OCFL.Repository.Test.FS.Remote", remote ? "true" : "false");
        return properties;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MCROCFLFileSystemProvider.get().init();
        MCRTransactionHelper.beginTransaction(MCROCFLFileSystemTransaction.class);
        MCROCFLTestCaseHelper.loadDerivate(DERIVATE_1);
        MCRTransactionHelper.commitTransaction(MCROCFLFileSystemTransaction.class);
    }

    @Override
    public void tearDown() throws Exception {
        MCROCFLFileSystemProvider.get().clearCache();
        MCRTransactionHelper.rollbackTransaction();
        MCROCFLFileSystemTransaction.resetTransactionCounter();
        super.tearDown();
    }

}
