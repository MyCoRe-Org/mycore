/**
 * 
 */
package org.mycore.backend.jpa;

import org.mycore.common.events.MCRShutdownHandler.Closeable;

/**
 * Closes {@link MCREntityManagerProvider#getEntityManagerFactory()}
 * @author Thomas Scheffler (yagee)
 */
class MCRJPAShutdownProcessor implements Closeable {

    @Override
    public int getPriority() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void close() {
        MCREntityManagerProvider.getEntityManagerFactory().close();
    }

}
