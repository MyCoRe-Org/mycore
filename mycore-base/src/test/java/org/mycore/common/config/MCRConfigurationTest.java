/**
 * 
 */
package org.mycore.common.config;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRConfigurationTest extends MCRTestCase {

    @Test
    public final void testDeprecatedProperties() {
        String deprecatedProperty = "MCR.nameOfProject";
        String validProperty = "MCR.NameOfProject";
        MCRConfiguration config = MCRConfiguration.instance();
        assertEquals(config.getString(validProperty), config.getString(deprecatedProperty));
    }

}
