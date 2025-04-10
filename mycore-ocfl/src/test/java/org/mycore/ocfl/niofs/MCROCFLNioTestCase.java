package org.mycore.ocfl.niofs;

import java.util.Arrays;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mycore.common.MCRTransactionManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.ocfl.MCROCFLTestCase;
import org.mycore.ocfl.MCROCFLTestCaseHelper;
import org.mycore.ocfl.util.MCROCFLDeleteUtils;
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

    private final boolean purge;

    @Parameterized.Parameters(name = "remote: {0}, purge: {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] { { false, false }, { false, true }, { true, false }, { true, true } });
    }

    public MCROCFLNioTestCase(boolean remote, boolean purge) {
        this.remote = remote;
        this.purge = purge;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // set remote
        this.setUpRepository(remote);
        // set purge
        String purgePropertyName
            = MCROCFLDeleteUtils.PROPERTY_PREFIX + MCROCFLObjectIDPrefixHelper.MCRDERIVATE.replace(":", "");
        MCRConfiguration2.set(purgePropertyName, Boolean.toString(purge));
        // setup
        MCROCFLFileSystemProvider.get().init();
        MCRTransactionManager.beginTransactions(MCROCFLFileSystemTransaction.class);
        MCROCFLTestCaseHelper.loadDerivate(DERIVATE_1);
        MCRTransactionManager.commitTransactions(MCROCFLFileSystemTransaction.class);
    }

    @Override
    public void tearDown() throws Exception {
        MCROCFLFileSystemProvider.get().clearCache();
        MCRTransactionManager.rollbackTransactions();
        MCROCFLFileSystemTransaction.resetTransactionCounter();
        super.tearDown();
    }

    public boolean isRemote() {
        return remote;
    }

    public boolean isPurge() {
        return purge;
    }

}
