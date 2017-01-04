/**
 * 
 */
package org.mycore.common.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRConfigurationTest extends MCRTestCase {

    @Test(expected = MCRConfigurationException.class)
    public final void testDeprecatedProperties() {
        String deprecatedProperty = "MCR.nameOfProject";
        MCRConfiguration config = MCRConfiguration.instance();
        config.getString(deprecatedProperty, "MyCoRe");
    }

}
